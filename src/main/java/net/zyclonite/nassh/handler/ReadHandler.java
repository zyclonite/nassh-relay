/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2020   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.Session;
import net.zyclonite.nassh.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
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
        final HttpServerResponse response = context.response();
        response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.putHeader("Pragma", "no-cache");
        if (request.params().contains("sid") && request.params().contains("rcnt")) {
            final UUID sid = UUID.fromString(request.params().get("sid"));
            final LocalMap<String, Session> map = vertx.sharedData().getLocalMap(Constants.SESSIONS);
            final Session session = map.get(sid.toString());
            if (session == null) {
                logger.warn("could not find valid session for " + sid);
                response.setStatusCode(410);
                response.end();
                return;
            }
            session.setRead_count(Integer.parseInt(request.params().get("rcnt")));

            final TransferQueue queue;
            try {
                queue = QueueFactory.getQueue(sid.toString());
            } catch (NoSuchQueueException ex) {
                logger.warn(ex.getMessage(), ex.fillInStackTrace());
                response.setStatusCode(410);
                response.end();
                return;
            }
            final Buffer buffer = queue.poll();
            if (buffer == null) {
                queue.addObserver(new TransferObserver(session, request));
            } else {
                final String encodedBytes = Base64.getUrlEncoder().encodeToString(buffer.getBytes());
                response.setStatusCode(200);
                response.end(encodedBytes);
            }
        } else {
            response.setStatusCode(410);
            response.end();
        }
    }
}
