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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.util.AuthSessionManager;
import net.zyclonite.nassh.util.Constants;
import net.zyclonite.nassh.util.RequestHelper;
import net.zyclonite.nassh.util.WebHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

/**
 * @author zyclonite
 */
public class CookieHandler implements Handler<RoutingContext> {

    private static final Logger logger = LogManager.getLogger();
    private static final String STATIC_FILE = "/webroot/auth.html";
    private final boolean authentication;
    private final boolean secureCookie;
    private final int sessionTTL;
    private final JsonObject auth;
    private final Random rng = new SecureRandom();

    public CookieHandler(final JsonObject config) {
        this.authentication = config.getBoolean("authentication", true);
        this.secureCookie = config.getBoolean("secure-cookie", true);
        this.sessionTTL = config.getInteger("auth-session-timeout", 600);
        this.auth = config.getJsonObject("auth");
    }

    @Override
    public void handle(final RoutingContext context) {
        logger.debug(() -> "got request");
        var request = context.request();
        var response = context.response();
        response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.putHeader("Pragma", "no-cache");
        if (request.params().contains("ext") && request.params().contains("path")) {
            var ext = request.params().get("ext");
            var path = request.params().get("path");
            if (!authentication) {
                response.putHeader("location", "chrome-extension://" + ext + "/" + path + "#anonymous@" + RequestHelper.getHost(request));
                response.setStatusCode(302);
                response.end();
                return;
            }
            var authSession = WebHelper.validateCookie(context);
            if (authSession != null) {
                var gplusid = authSession.get("id");
                response.putHeader("location", "chrome-extension://" + ext + "/" + path + "#" + gplusid + "@" + RequestHelper.getHost(request));
                response.setStatusCode(302);
                response.end();
            } else {
                response.setStatusCode(200);
                var state = new BigInteger(130, rng).toString(32);
                var session = AuthSessionManager.createSession(sessionTTL);
                session.put("state", state);
                var sessionCookie = Cookie
                    .cookie(Constants.SESSIONCOOKIE, session.getId().toString())
                    .setHttpOnly(true);
                if (secureCookie) {
                    sessionCookie
                        .setSameSite(CookieSameSite.NONE)
                        .setSecure(true);
                }
                response.addCookie(sessionCookie);
                var auth_html = new Scanner(Objects.requireNonNull(this.getClass().getResourceAsStream(STATIC_FILE)), StandardCharsets.UTF_8)
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
