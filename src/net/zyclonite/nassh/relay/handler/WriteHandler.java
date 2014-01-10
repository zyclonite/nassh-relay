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
public class WriteHandler implements Handler<HttpServerRequest> {

    private static final Log LOG = LogFactory.getLog(WriteHandler.class);

    @Override
    public void handle(final HttpServerRequest request) {
        request.response().putHeader("Access-Control-Allow-Origin", "chrome-extension://pnhechapfaindjhompbnflcldabbghjo");
        request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        request.response().putHeader("Pragma", "no-cache");
        if (request.params().contains("sid") && request.params().contains("wcnt") && request.params().contains("data")) {
            final UUID sid = UUID.fromString(request.params().get("sid"));
            final byte[] data = Base64.decodeBase64(request.params().get("data"));
            request.response().setStatusCode(200);
            final ConcurrentMap<String, Session> map = VertxPlatform.getInstance().getSharedData().getMap(Constants.SESSIONS);
            if (!map.containsKey(sid.toString())) {
                request.response().setStatusCode(410);
                request.response().end();
                return;
            }
            final Session session = map.get(sid.toString());
            session.setWrite_count(Integer.parseInt(request.params().get("wcnt")));
            final Buffer message = new Buffer();
            message.appendBytes(data);
            VertxPlatform.getInstance().getEventBus().publish(session.getHandler(), message);
            request.response().end();
        } else {
            request.response().setStatusCode(410);
            request.response().end();
        }
    }
}
