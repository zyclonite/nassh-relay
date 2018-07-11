/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2018   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.Session;
import net.zyclonite.nassh.util.*;
import org.apache.commons.codec.binary.Base64;

import java.util.UUID;

/**
 * @author zyclonite
 */
public class ReadHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(ReadHandler.class);
    private final Vertx vertx;

    public ReadHandler(final Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void handle(final RoutingContext context) {
        final HttpServerRequest request = context.request();
        WebHelper.putAccessControlAllowHeader(request);
        request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        request.response().putHeader("Pragma", "no-cache");
        if (request.params().contains("sid") && request.params().contains("rcnt")) {
            final UUID sid = UUID.fromString(request.params().get("sid"));
            final LocalMap<String, Session> map = vertx.sharedData().getLocalMap(Constants.SESSIONS);
            final Session session = map.get(sid.toString());
            if (session == null) {
                logger.warn("could not find valid session for " + sid);
                request.response().setStatusCode(410);
                request.response().end();
                return;
            }
            session.setRead_count(Integer.parseInt(request.params().get("rcnt")));

            final TransferQueue queue;
            try {
                queue = QueueFactory.getQueue(sid.toString());
            } catch (NoSuchQueueException ex) {
                logger.warn(ex, ex.fillInStackTrace());
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
