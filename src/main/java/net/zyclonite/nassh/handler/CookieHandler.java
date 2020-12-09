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
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.AuthSession;
import net.zyclonite.nassh.util.AuthSessionManager;
import net.zyclonite.nassh.util.Constants;
import net.zyclonite.nassh.util.RequestHelper;
import net.zyclonite.nassh.util.WebHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;

/**
 * @author zyclonite
 */
public class CookieHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(CookieHandler.class);
    private static final String STATIC_FILE = "/webroot/auth.html";
    private final boolean authentication;
    private final boolean secureCookie;
    private final int sessionTTL;
    private final JsonObject auth;

    public CookieHandler(final JsonObject config) {
        this.authentication = config.getBoolean("authentication", true);
        this.secureCookie = config.getBoolean("secure-cookie", true);
        this.sessionTTL = config.getInteger("auth-session-timeout", 600);
        this.auth = config.getJsonObject("auth");
    }

    @Override
    public void handle(final RoutingContext context) {
        logger.debug("got request");
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();
        response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.putHeader("Pragma", "no-cache");
        if (request.params().contains("ext") && request.params().contains("path")) {
            final String ext = request.params().get("ext");
            final String path = request.params().get("path");
            if (!authentication) {
                response.putHeader("location", "chrome-extension://" + ext + "/" + path + "#anonymous@" + RequestHelper.getHost(request));
                response.setStatusCode(302);
                response.end();
                return;
            }
            final AuthSession authSession = WebHelper.validateCookie(context);
            if (authSession != null) {
                final String gplusid = authSession.get("id");
                response.putHeader("location", "chrome-extension://" + ext + "/" + path + "#" + gplusid + "@" + RequestHelper.getHost(request));
                response.setStatusCode(302);
                response.end();
            } else {
                response.setStatusCode(200);
                final String state = new BigInteger(130, new SecureRandom()).toString(32);
                final AuthSession session = AuthSessionManager.createSession(sessionTTL);
                session.put("state", state);
                final Cookie sessionCookie = Cookie
                    .cookie(Constants.SESSIONCOOKIE, session.getId().toString())
                    .setHttpOnly(true);
                if (secureCookie) {
                    sessionCookie
                        .setSameSite(CookieSameSite.NONE)
                        .setSecure(true);
                }
                response.addCookie(sessionCookie);
                final String auth_html = new Scanner(this.getClass().getResourceAsStream(STATIC_FILE), "UTF-8")
                    .useDelimiter("\\A").next()
                    .replaceAll("[{]{2}\\s*CLIENT_ID\\s*[}]{2}", auth.getString("client-id"))
                    .replaceAll("[{]{2}\\s*STATE\\s*[}]{2}", state)
                    .replaceAll("[{]{2}\\s*APPLICATION_NAME\\s*[}]{2}", auth.getString("title"));
                response.end(auth_html);
            }
        } else {
            response.setStatusCode(401);
            response.end("unauthorized");
        }
    }
}
