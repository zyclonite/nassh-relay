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
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.Session;
import net.zyclonite.nassh.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Base64;
import java.util.UUID;

/**
 * @author zyclonite
 */
public class ReadHandler implements Handler<RoutingContext> {

    private static final Logger logger = LogManager.getLogger();
    private final Vertx vertx;

    public ReadHandler(final Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void handle(final RoutingContext context) {
        var request = context.request();
        var response = context.response();
        response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.putHeader("Pragma", "no-cache");
        if (request.params().contains("sid") && request.params().contains("rcnt")) {
            var sid = UUID.fromString(request.params().get("sid"));
            var map = vertx.sharedData().<String, Session>getLocalMap(Constants.SESSIONS);
            var session = map.get(sid.toString());
            if (session == null) {
                logger.warn(() -> "could not find valid session for " + sid);
                response.setStatusCode(410);
                response.end();
                return;
            }
            session.setRead_count(Integer.parseInt(request.params().get("rcnt")));

            final TransferQueue queue;
            try {
                queue = QueueFactory.getQueue(sid.toString());
            } catch (NoSuchQueueException ex) {
                logger.warn(() -> ex);
                response.setStatusCode(410);
                response.end();
                return;
            }
            var buffer = queue.poll();
            if (buffer == null) {
                queue.addObserver(new TransferObserver(session, request));
            } else {
                var encodedBytes = Base64.getUrlEncoder().encodeToString(buffer.getBytes());
                response.setStatusCode(200);
                response.end(encodedBytes);
            }
        } else {
            response.setStatusCode(410);
            response.end();
        }
    }
}
