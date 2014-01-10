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
import net.zyclonite.nassh.relay.util.TransferObserver;
import net.zyclonite.nassh.relay.util.TransferQueue;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

/**
 *
 * @author zyclonite
 */
public class ReadHandler implements Handler<HttpServerRequest> {

    private static final Log LOG = LogFactory.getLog(ReadHandler.class);

    @Override
    public void handle(final HttpServerRequest request) {
        request.response().putHeader("Access-Control-Allow-Origin", "chrome-extension://pnhechapfaindjhompbnflcldabbghjo");
        request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        request.response().putHeader("Pragma", "no-cache");
        if (request.params().contains("sid") && request.params().contains("rcnt")) {
            final UUID sid = UUID.fromString(request.params().get("sid"));
            final ConcurrentMap<String, Session> map = VertxPlatform.getInstance().getSharedData().getMap(Constants.SESSIONS);
            if (!map.containsKey(sid.toString())) {
                request.response().setStatusCode(410);
                request.response().end();
                return;
            }
            final Session session = map.get(sid.toString());
            session.setRead_count(Integer.parseInt(request.params().get("rcnt")));

            final TransferQueue queue;
            try {
                queue = QueueFactory.getQueue(sid.toString());
            } catch (NoSuchQueueException ex) {
                LOG.warn(ex, ex.fillInStackTrace());
                request.response().setStatusCode(410);
                request.response().end();
                return;
            }
            final Buffer buffer = queue.poll();
            if (buffer == null) {
                queue.addObserver(new TransferObserver(session, request));
            } else {
                final String encodedBytes = Base64.encodeBase64URLSafeString(buffer.getBytes());
                request.response().setStatusCode(200);
                request.response().end(encodedBytes);
            }
        } else {
            request.response().setStatusCode(410);
            request.response().end();
        }
    }
}
