/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.handler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import net.zyclonite.nassh.relay.model.Session;
import net.zyclonite.nassh.relay.service.VertxPlatform;
import net.zyclonite.nassh.relay.util.AccessHelper;
import net.zyclonite.nassh.relay.util.AppConfig;
import net.zyclonite.nassh.relay.util.Constants;
import net.zyclonite.nassh.relay.util.WebHelper;
import net.zyclonite.nassh.relay.util.NoSuchQueueException;
import net.zyclonite.nassh.relay.util.QueueFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;

/**
 *
 * @author zyclonite
 */
public class ProxyHandler implements Handler<HttpServerRequest> {

    private static final Log LOG = LogFactory.getLog(ProxyHandler.class);
    private static final AppConfig CONFIG = AppConfig.getInstance();
    private final ConcurrentMap<String, Session> sessions;
    private final int sessionlimit;
    private final boolean authentication;

    public ProxyHandler() {
        authentication = CONFIG.getBoolean("application.authentication", true);
        sessions = VertxPlatform.getInstance().getSharedData().getMap(Constants.SESSIONS);
        sessionlimit = CONFIG.getInt("application.max-sessions", 100);
    }

    @Override
    public void handle(final HttpServerRequest request) {
        LOG.debug("got request");
        String gplusid = null;
        WebHelper.putAccessControlAllowHeader(request);
        request.response().putHeader("Access-Control-Allow-Credentials", "true");
        request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        request.response().putHeader("Pragma", "no-cache");
        if (request.params().contains("host") && request.params().contains("port")) {
            if (authentication) {
                gplusid = WebHelper.validateCookie(request);
                if (gplusid == null) {
                    request.response().setStatusCode(410);
                    request.response().end("session invalid");
                    return;
                }
            }
            final String host = request.params().get("host");
            final int port = Integer.parseInt(request.params().get("port"));
            final UUID sid = UUID.randomUUID();
            final String clienthost;
            if (request.headers().contains("X-Real-IP")) {
                clienthost = request.headers().get("X-Real-IP");
            } else {
                clienthost = request.remoteAddress().getHostString();
            }
            final InetAddress address;
            try {
                address = InetAddress.getByName(host);
            } catch (UnknownHostException ex) {
                request.response().setStatusCode(410);
                request.response().end("invalid host");
                return;
            }
            if (!AccessHelper.isHostAllowed(address, gplusid)) {
                request.response().setStatusCode(410);
                request.response().end("host not allowed");
                LOG.warn("client " + clienthost + " " + (gplusid == null ? "" : "(" + gplusid + ")") + "tried to access " + address.getHostAddress() + " but was not allowed");
                return;
            }
            if (sessions.size() >= sessionlimit) {
                request.response().setStatusCode(410);
                request.response().end("session limit reached");
                LOG.warn("ssh session limit of " + sessionlimit + " reached");
                return;
            }
            request.response().setStatusCode(200);
            connectTcpEndpoint(sid, address.getHostAddress(), port, clienthost);
            request.response().end(sid.toString());
        } else {
            request.response().setStatusCode(410);
            request.response().end("error");
        }
    }

    private void connectTcpEndpoint(final UUID sid, final String host, final int port, final String clienthost) {
        final NetClient client = VertxPlatform.getInstance().createNetClient();
        client.setReconnectAttempts(10);
        client.setReconnectInterval(500);
        client.connect(port, host, new AsyncResultHandler<NetSocket>() {
            @Override
            public void handle(AsyncResult<NetSocket> asyncResult) {
                if (asyncResult.succeeded()) {
                    LOG.info("Connected to ssh server: " + host + ":" + port + " (" + clienthost + ")");
                    QueueFactory.createQueue(sid.toString());
                    asyncResult.result().dataHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(final Buffer buffer) {
                            try {
                                QueueFactory.getQueue(sid.toString()).add(buffer);
                            } catch (NoSuchQueueException ex) {
                                LOG.warn(ex, ex.fillInStackTrace());
                            }
                        }
                    });
                    asyncResult.result().closeHandler(new VoidHandler() {
                        @Override
                        public void handle() {
                            LOG.info("ssh server connection closed " + host + ":" + port);
                            QueueFactory.deleteQueue(sid.toString());
                            sessions.remove(sid.toString());
                        }
                    });
                    final Session session = new Session();
                    session.setHandler(asyncResult.result().writeHandlerID());
                    sessions.put(sid.toString(), session);
                    registerTimerOut(session, client);
                } else {
                    LOG.warn("Could not connect to ssh server: " + asyncResult.cause().getMessage(), asyncResult.cause().fillInStackTrace());
                }
            }
        });
    }

    private void registerTimerOut(final Session session, final NetClient client) {
        VertxPlatform.getInstance().setPeriodic(CONFIG.getInt("application.tcp-session-timeout", 60)*1000, new Handler<Long>() {
            private int readCount = 0;
            private int writeCount = 0;
            @Override
            public void handle(Long timerID) {
                if((session.getRead_count() <= readCount) && (session.getWrite_count() <= writeCount)) {
                    if(client != null) {
                        client.close();
                    }
                    VertxPlatform.getInstance().cancelTimer(timerID);
                }
                readCount = session.getRead_count();
                writeCount = session.getWrite_count();
            }
        });
    }
}
