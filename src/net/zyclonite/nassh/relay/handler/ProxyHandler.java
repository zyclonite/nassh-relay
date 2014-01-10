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

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import net.zyclonite.nassh.relay.model.Session;
import net.zyclonite.nassh.relay.service.VertxPlatform;
import net.zyclonite.nassh.relay.util.Constants;
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

    @Override
    public void handle(final HttpServerRequest request) {
        LOG.debug("got request");
        if (request.params().contains("host") && request.params().contains("port")) {
            final String host = request.params().get("host");
            final int port = Integer.parseInt(request.params().get("port"));
            final UUID sid = UUID.randomUUID();
            request.response().putHeader("Access-Control-Allow-Origin", "chrome-extension://pnhechapfaindjhompbnflcldabbghjo");
            request.response().putHeader("Access-Control-Allow-Credentials", "true");
            request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            request.response().putHeader("Pragma", "no-cache");
            request.response().setStatusCode(200);
            connectTcpEndpoint(sid, host, port);
            request.response().end(sid.toString());
        } else {
            request.response().setStatusCode(410);
            request.response().end("error");
        }
    }

    private void connectTcpEndpoint(final UUID sid, final String host, final int port) {
        final NetClient client = VertxPlatform.getInstance().createNetClient();
        client.setReconnectAttempts(10);
        client.setReconnectInterval(500);
        client.connect(port, host, new AsyncResultHandler<NetSocket>() {
            @Override
            public void handle(AsyncResult<NetSocket> asyncResult) {
                if (asyncResult.succeeded()) {
                    LOG.debug("Connected to ssh server: " + host + ":" + port);
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
                            LOG.debug("ssh server connection closed");
                            QueueFactory.deleteQueue(sid.toString());
                        }
                    });
                    final ConcurrentMap<String, Session> map = VertxPlatform.getInstance().getSharedData().getMap(Constants.SESSIONS);
                    final Session session = new Session();
                    session.setHandler(asyncResult.result().writeHandlerID());
                    map.put(sid.toString(), session);
                } else {
                    LOG.warn("Could not connect to ssh server: " + asyncResult.cause().getMessage(), asyncResult.cause().fillInStackTrace());
                }
            }
        });
    }
}
