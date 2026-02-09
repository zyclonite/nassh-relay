/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2020   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import net.zyclonite.nassh.model.Session;

import java.util.Base64;

/**
 * @author zyclonite
 */
public class TransferObserver {

    private final Object request;
    private final Session session;

    public TransferObserver(final Session session, final Object request) {
        this.request = request;
        this.session = session;
    }

    public void update(final TransferQueue queue) {
        if (request instanceof HttpServerRequest req) {
            var buffer = queue.poll();
            queue.deleteObserver(this);
            assert buffer != null;
            var encodedBytes = Base64.getUrlEncoder().encodeToString(buffer.getBytes());
            req.response().setStatusCode(200);
            req.response().end(encodedBytes);
        } else if (request instanceof ServerWebSocket ws) {
            var buffer = queue.poll();
            if (!ws.writeQueueFull()) {
                var ackbuffer = Buffer.buffer();
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
