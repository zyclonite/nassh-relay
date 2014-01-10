/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.util;

import java.util.Observable;
import java.util.Observer;
import net.zyclonite.nassh.relay.model.Session;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;

/**
 *
 * @author zyclonite
 */
public class TransferObserver implements Observer {

    private static final Log LOG = LogFactory.getLog(TransferObserver.class);
    private HttpServerRequest request = null;
    private ServerWebSocket websocket = null;
    private Session session = null;

    public TransferObserver(final Session session, final HttpServerRequest request) {
        this.request = request;
        this.session = session;
    }

    public TransferObserver(final Session session, final ServerWebSocket websocket) {
        this.websocket = websocket;
        this.session = session;
    }

    @Override
    public void update(final Observable queue, final Object arg) {
        final Buffer buffer = ((TransferQueue) queue).poll();
        if (request != null) {
            queue.deleteObserver(this);
            final String encodedBytes = Base64.encodeBase64URLSafeString(buffer.getBytes());
            request.response().setStatusCode(200);
            request.response().end(encodedBytes);
        }else if (websocket != null) {
            final Buffer ackbuffer = new Buffer();
            ackbuffer.setInt(0, session.getWrite_count());
            ackbuffer.setBuffer(4, buffer);
            websocket.write(ackbuffer);
        }else{
            queue.deleteObserver(this);
        }
    }
}
