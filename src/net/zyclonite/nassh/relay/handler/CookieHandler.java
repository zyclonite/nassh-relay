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

import io.netty.handler.codec.http.ServerCookieEncoder;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;
import net.zyclonite.nassh.relay.model.AuthSession;
import net.zyclonite.nassh.relay.util.AppConfig;
import net.zyclonite.nassh.relay.util.AuthSessionManager;
import net.zyclonite.nassh.relay.util.Constants;
import net.zyclonite.nassh.relay.util.CookieHelper;
import net.zyclonite.nassh.relay.verticle.WebService;
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
    private static final String STATIC_FILE = "/net/zyclonite/nassh/relay/html/auth.html";
    private final AppConfig config;
    private final boolean authentication;

    public CookieHandler() {
        config = AppConfig.getInstance();
        authentication = config.getBoolean("application.authentication", true);
    }

    @Override
    public void handle(final HttpServerRequest request) {
        LOG.debug("got request");
        request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        request.response().putHeader("Pragma", "no-cache");
        if (request.params().contains("ext") && request.params().contains("path")) {
            final String ext = request.params().get("ext");
            final String path = request.params().get("path");
            if(!authentication){
                request.response().putHeader("location", "chrome-extension://" + ext + "/" + path + "#anonymous@" + config.getString("application.relay-url", "localhost:8080"));
                request.response().setStatusCode(302);
                request.response().end();
                return;
            }
            final String gplusid = CookieHelper.validateCookie(request);
            if (gplusid != null) {
                request.response().putHeader("location", "chrome-extension://" + ext + "/" + path + "#" + gplusid + "@" + config.getString("application.relay-url", "localhost:8080"));
                request.response().setStatusCode(302);
                request.response().end();
            } else {
                request.response().setStatusCode(200);
                final String state = new BigInteger(130, new SecureRandom()).toString(32);
                final AuthSession session = AuthSessionManager.createSession();
                session.put("state", state);
                request.response().putHeader("Set-Cookie", ServerCookieEncoder.encode(Constants.SESSIONCOOKIE, session.getId().toString()));
                final String auth_html = new Scanner(this.getClass().getResourceAsStream(STATIC_FILE), "UTF-8")
                        .useDelimiter("\\A").next()
                        .replaceAll("[{]{2}\\s*CLIENT_ID\\s*[}]{2}", WebService.CLIENT_ID)
                        .replaceAll("[{]{2}\\s*STATE\\s*[}]{2}", state)
                        .replaceAll("[{]{2}\\s*APPLICATION_NAME\\s*[}]{2}", WebService.APPLICATION_NAME)
                        .toString();
                request.response().end(auth_html);
            }
        } else {
            request.response().setStatusCode(401);
            request.response().end("unauthorized");
        }
    }
}