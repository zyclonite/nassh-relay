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
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.Session;
import net.zyclonite.nassh.util.Constants;

import java.util.Base64;
import java.util.UUID;

/**
 * @author zyclonite
 */
public class WriteHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(WriteHandler.class);
    private final Vertx vertx;

    public WriteHandler(final Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void handle(final RoutingContext context) {
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();
        response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.putHeader("Pragma", "no-cache");
        if (request.params().contains("sid") && request.params().contains("wcnt") && request.params().contains("data")) {
            final UUID sid = UUID.fromString(request.params().get("sid"));
            final byte[] data = Base64.getUrlDecoder().decode(request.params().get("data"));
            response.setStatusCode(200);
            final LocalMap<String, Session> map = vertx.sharedData().getLocalMap(Constants.SESSIONS);
            final Session session = map.get(sid.toString());
            if (session == null) {
                response.setStatusCode(410);
                response.end();
                return;
            }
            session.setWrite_count(Integer.parseInt(request.params().get("wcnt")));
            final Buffer message = Buffer.buffer();
            message.appendBytes(data);
            vertx.eventBus().publish(session.getHandler(), message);
            response.end();
        } else {
            response.setStatusCode(410);
            response.end();
        }
    }
}
