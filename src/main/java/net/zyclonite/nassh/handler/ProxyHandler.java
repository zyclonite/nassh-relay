/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2023   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.handler;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.AuthSession;
import net.zyclonite.nassh.model.Session;
import net.zyclonite.nassh.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.util.UUID;

/**
 * @author zyclonite
 */
public class ProxyHandler implements Handler<RoutingContext> {

    private static final Logger logger = LogManager.getLogger();
    private final LocalMap<String, Session> sessions;
    private final int sessionlimit;
    private final boolean authentication;
    private final VertxInternal vertx;
    private final NetClient client;
    private final JsonObject config;
    private final JsonArray accessList;
    private final JsonArray whiteList;
    private final JsonArray blackList;

    public ProxyHandler(final Vertx vertx, final JsonObject config) {
        this.vertx = (VertxInternal) vertx;
        this.client = vertx.createNetClient(new NetClientOptions().setRegisterWriteHandler(true).setReconnectAttempts(10).setReconnectInterval(500));
        this.config = config;
        this.authentication = config.getJsonObject("application").getBoolean("authentication", true);
        this.sessions = vertx.sharedData().getLocalMap(Constants.SESSIONS);
        this.sessionlimit = config.getJsonObject("application").getInteger("max-sessions", 100);
        this.accessList = config.getJsonArray("accesslist", new JsonArray());
        this.whiteList = config.getJsonArray("whitelist", new JsonArray());
        this.blackList = config.getJsonArray("blacklist", new JsonArray());
    }

    @Override
    public void handle(final RoutingContext context) {
        logger.debug(() -> "got request");
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();
        response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.putHeader("Pragma", "no-cache");
        if (request.params().contains("host") && request.params().contains("port")) {
            AuthSession authPreSession = null;
            if (authentication) {
                authPreSession = WebHelper.validateCookie(context);
                if (authPreSession == null) {
                    response.setStatusCode(410);
                    response.end("session invalid");
                    return;
                }
            }
            final AuthSession authSession = authPreSession;
            final String host = request.params().get("host");
            final int port = Integer.parseInt(request.params().get("port"));
            final UUID sid = UUID.randomUUID();
            final String clienthost = RequestHelper.getRemoteHost(request);
            if (sessions.size() >= sessionlimit) {
                response.setStatusCode(410);
                response.end("session limit reached");
                logger.warn(() -> "ssh session limit of " + sessionlimit + " reached");
                return;
            }
            vertx.nameResolver().resolve(host).andThen(result -> {
                if (result.succeeded()) {
                    final InetAddress address = result.result();
                    vertx.executeBlocking(() -> AccessHelper.isHostAllowed(accessList, whiteList, blackList, address, authSession), false)
                        .onSuccess(isAllowed -> {
                            if (!isAllowed) {
                                response.setStatusCode(410);
                                response.end("host not allowed");
                                logger.warn(() -> "client " + clienthost + " " + (authSession == null ? "" : "(" + authSession + ")") + " tried to access " + address.getHostAddress() + " but was not allowed");
                            } else {
                                connectTcpEndpoint(sid, address.getHostAddress(), port, clienthost).future().onComplete(ar -> {
                                    if (ar.succeeded()) {
                                        response.setStatusCode(200);
                                        response.end(sid.toString());
                                    } else {
                                        response.setStatusCode(500);
                                        response.end("could not init ssh session");
                                    }
                                });
                            }
                        })
                        .onFailure(t -> {
                            logger.error(t);
                            response.setStatusCode(500);
                            response.end("internal server error");
                        });
                } else {
                    response.setStatusCode(410);
                    response.end("invalid host");
                }
            });
        } else {
            response.setStatusCode(410);
            response.end("error");
        }
    }

    private Promise<UUID> connectTcpEndpoint(final UUID sid, final String host, final int port, final String clienthost) {
        final Promise<UUID> promise = Promise.promise();
        client.connect(port, host).andThen(asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.info(() -> "Connected to ssh server: " + host + ":" + port + " (" + clienthost + ")");
                QueueFactory.createQueue(sid.toString());
                asyncResult.result().drainHandler(v -> asyncResult.result().resume());
                asyncResult.result().handler(buffer -> {
                    try {
                        final TransferQueue queue = QueueFactory.getQueue(sid.toString());
                        if (!queue.isFull()) {
                            queue.add(buffer);
                        } else {
                            asyncResult.result().pause();
                        }
                    } catch (NoSuchQueueException ex) {
                        logger.warn(() -> ex);
                    }
                });
                asyncResult.result().closeHandler(v -> {
                    logger.info(() -> "ssh server connection closed " + host + ":" + port);
                    QueueFactory.deleteQueue(sid.toString());
                    sessions.remove(sid.toString());
                });
                final Session session = new Session();
                session.setHandler(asyncResult.result().writeHandlerID());
                sessions.put(sid.toString(), session);
                registerTimerOut(session, client);
                promise.complete(sid);
            } else {
                promise.fail(asyncResult.cause());
                logger.warn(() -> "Could not connect to ssh server: " + asyncResult.cause().getMessage(), asyncResult.cause());
            }
        });
        return promise;
    }

    private void registerTimerOut(final Session session, final NetClient client) {
        vertx.setPeriodic(config.getJsonObject("application").getInteger("tcp-session-timeout", 1200) * 1000, new Handler<>() {
            private int readCount = 0;
            private int writeCount = 0;

            @Override
            public void handle(Long timerID) {
                if ((session.getRead_count() <= readCount) && (session.getWrite_count() <= writeCount)) {
                    session.setActive(false);
                    if (client != null) {
                        client.close();
                    }
                    vertx.cancelTimer(timerID);
                }
                readCount = session.getRead_count();
                writeCount = session.getWrite_count();
            }
        });
    }
}
