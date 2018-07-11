/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2018   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.zyclonite.nassh.model.Session;
import org.apache.commons.codec.binary.Base64;

import java.util.Observable;
import java.util.Observer;

/**
 * @author zyclonite
 */
public class TransferObserver implements Observer {

    private static Logger logger = LoggerFactory.getLogger(TransferObserver.class);
    private Object request = null;
    private Session session = null;

    public TransferObserver(final Session session, final Object request) {
        this.request = request;
        this.session = session;
    }

    @Override
    public void update(final Observable queue, final Object arg) {
        if (request instanceof HttpServerRequest) {
            final Buffer buffer = ((TransferQueue) queue).poll();
            queue.deleteObserver(this);
            final HttpServerRequest req = (HttpServerRequest) request;
            assert buffer != null;
            final String encodedBytes = Base64.encodeBase64URLSafeString(buffer.getBytes());
            req.response().setStatusCode(200);
            req.response().end(encodedBytes);
        } else if (request instanceof ServerWebSocket) {
            final Buffer buffer = ((TransferQueue) queue).poll();
            final ServerWebSocket ws = (ServerWebSocket) request;
            if (!ws.writeQueueFull()) {
                final Buffer ackbuffer = Buffer.buffer();
                ackbuffer.setInt(0, session.getWrite_count());
                ackbuffer.setBuffer(4, buffer);
                ws.write(ackbuffer);
            } else {
                ws.pause();
            }
        } else {
            queue.deleteObserver(this);
        }
    }
}
