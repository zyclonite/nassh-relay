/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2016   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VoidHandler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.AuthSession;
import net.zyclonite.nassh.model.Session;
import net.zyclonite.nassh.util.*;

import java.net.InetAddress;
import java.util.UUID;

/**
 *
 * @author zyclonite
 */
public class ProxyHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final LocalMap<String, Session> sessions;
    private final int sessionlimit;
    private final boolean authentication;
    private final Vertx vertx;
    private final JsonObject config;

    public ProxyHandler(final Vertx vertx, final JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        this.authentication = config.getJsonObject("application").getBoolean("authentication", true);
        this.sessions = vertx.sharedData().getLocalMap(Constants.SESSIONS);
        this.sessionlimit = config.getJsonObject("application").getInteger("max-sessions", 100);
    }

    @Override
    public void handle(final RoutingContext context) {
        logger.debug("got request");
        final HttpServerRequest request = context.request();
        WebHelper.putAccessControlAllowHeader(request);
        request.response().putHeader("Access-Control-Allow-Credentials", "true");
        request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        request.response().putHeader("Pragma", "no-cache");
        if (request.params().contains("host") && request.params().contains("port")) {
            AuthSession authPreSession = null;
            if (authentication) {
                authPreSession = WebHelper.validateCookie(context);
                if (authPreSession == null) {
                    request.response().setStatusCode(410);
                    request.response().end("session invalid");
                    return;
                }
            }
            final AuthSession authSession = authPreSession;
            final String host = request.params().get("host");
            final int port = Integer.parseInt(request.params().get("port"));
            final UUID sid = UUID.randomUUID();
            final String clienthost;
            if (request.headers().contains("X-Real-IP")) {
                clienthost = request.headers().get("X-Real-IP");
            } else {
                clienthost = request.remoteAddress().host();
            }
            if (sessions.size() >= sessionlimit) {
                request.response().setStatusCode(410);
                request.response().end("session limit reached");
                logger.warn("ssh session limit of " + sessionlimit + " reached");
                return;
            }
            ((VertxImpl)vertx).resolveAddress(host, result -> {
                if(result.succeeded()) {
                    final InetAddress address = result.result();
                    vertx.<Boolean>executeBlocking(future -> {
                        final boolean isAllowed = AccessHelper.isHostAllowed(config.getJsonArray("accesslist"), config.getJsonArray("blacklist"), address, authSession);
                        future.complete(isAllowed);
                    }, false, res -> {
                        if(res.succeeded()) {
                            if (!res.result()) {
                                request.response().setStatusCode(410);
                                request.response().end("host not allowed");
                                logger.warn("client " + clienthost + " " + (authSession == null ? "" : "(" + authSession + ")") + "tried to access " + address.getHostAddress() + " but was not allowed");
                            } else {
                                request.response().setStatusCode(200);
                                connectTcpEndpoint(sid, address.getHostAddress(), port, clienthost);
                                request.response().end(sid.toString());
                            }
                        }
                    });
                } else {
                    request.response().setStatusCode(410);
                    request.response().end("invalid host");
                }
            });
        } else {
            request.response().setStatusCode(410);
            request.response().end("error");
        }
    }

    private void connectTcpEndpoint(final UUID sid, final String host, final int port, final String clienthost) {
        final NetClient client = vertx.createNetClient(new NetClientOptions().setReconnectAttempts(10).setReconnectInterval(500));
        client.connect(port, host, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.info("Connected to ssh server: " + host + ":" + port + " (" + clienthost + ")");
                QueueFactory.createQueue(sid.toString());
                asyncResult.result().drainHandler(new VoidHandler() {
                    @Override
                    public void handle() {
                        asyncResult.result().resume();
                    }
                });
                asyncResult.result().handler(buffer -> {
                    try {
                        final TransferQueue queue = QueueFactory.getQueue(sid.toString());
                        if(!queue.isFull()) {
                            queue.add(buffer);
                        }else{
                            asyncResult.result().pause();
                        }
                    } catch (NoSuchQueueException ex) {
                        logger.warn(ex, ex.fillInStackTrace());
                    }
                });
                asyncResult.result().closeHandler(new VoidHandler() {
                    @Override
                    public void handle() {
                        logger.info("ssh server connection closed " + host + ":" + port);
                        QueueFactory.deleteQueue(sid.toString());
                        sessions.remove(sid.toString());
                    }
                });
                final Session session = new Session();
                session.setHandler(asyncResult.result().writeHandlerID());
                sessions.put(sid.toString(), session);
                registerTimerOut(session, client);
            } else {
                logger.warn("Could not connect to ssh server: " + asyncResult.cause().getMessage(), asyncResult.cause().fillInStackTrace());
            }
        });
    }

    private void registerTimerOut(final Session session, final NetClient client) {
        vertx.setPeriodic(config.getJsonObject("application").getInteger("tcp-session-timeout", 1200)*1000, new Handler<Long>() {
            private int readCount = 0;
            private int writeCount = 0;
            @Override
            public void handle(Long timerID) {
                if((session.getRead_count() <= readCount) && (session.getWrite_count() <= writeCount)) {
                    session.setActive(false);
                    if(client != null) {
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
