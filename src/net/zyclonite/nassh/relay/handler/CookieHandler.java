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

import net.zyclonite.nassh.relay.util.AppConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

/**
 *
 * @author zyclonite
 */
public class CookieHandler implements Handler<HttpServerRequest> {

    private static final Log LOG = LogFactory.getLog(CookieHandler.class);
    private final AppConfig config;

    public CookieHandler() {
        config = AppConfig.getInstance();
    }

    @Override
    public void handle(final HttpServerRequest request) {
        LOG.debug("got request");
        request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        request.response().putHeader("Pragma", "no-cache");
        if (request.params().contains("ext") && request.params().contains("path")) {
            final String ext = request.params().get("ext");
            final String path = request.params().get("path");
            final String user = "test";
            request.response().putHeader("location", "chrome-extension://" + ext + "/" + path + "#" + user + "@" + config.getString("application.relay-url", "localhost:8080"));
            request.response().setStatusCode(302);
            request.response().end();
        } else {
            request.response().setStatusCode(401);
            request.response().end("unauthorized");
        }
    }
}
