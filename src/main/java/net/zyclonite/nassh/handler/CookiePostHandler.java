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
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GoogleAuth;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.AuthSession;
import net.zyclonite.nassh.util.AuthSessionManager;
import net.zyclonite.nassh.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * @author zyclonite
 */
public class CookiePostHandler implements Handler<RoutingContext> {

    private static final Logger logger = LogManager.getLogger();
    private final OAuth2Auth oauth2;

    public CookiePostHandler(final Vertx vertx, final JsonObject config) {
        final JsonObject auth = config.getJsonObject("auth");
        this.oauth2 = GoogleAuth.create(vertx, auth.getString("client-id"), auth.getString("client-secret"));
    }

    @Override
    public void handle(final RoutingContext context) {
        logger.debug(() -> "got request");
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();
        response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.putHeader("Pragma", "no-cache");
        response.putHeader("Content-Type", "no-cache");
        response.putHeader("Content-Type", "application/json");
        final Cookie cookie = context.request().getCookie(Constants.SESSIONCOOKIE);
        UUID sessioncookie;
        if (cookie == null) {
            sessioncookie = null;
        } else {
            sessioncookie = UUID.fromString(cookie.getValue());
        }
        final AuthSession session = AuthSessionManager.getSession(sessioncookie);
        if (session == null) {
            response.setStatusCode(403);
            response.end("\"Invalid session cookie.\"");
            return;
        }
        final String token = session.get("token");
        final String state = session.get("state");
        if (token != null) {
            response.setStatusCode(200);
            response.end("\"Current user is already connected.\"");
            return;
        }
        if (!request.params().contains("state") || !request.params().get("state").equals(state)) {
            response.setStatusCode(403);
            response.end("\"Invalid state parameter.\"");
            return;
        }
        request.bodyHandler(body -> {
            final JsonObject tokenConfig = new JsonObject()
                .put("code", body.toString())
                .put("redirectUri", "postmessage");
            oauth2.authenticate(tokenConfig, ar -> {
                if (ar.succeeded() && ar.result() != null) {
                    final User user = ar.result();
                    final JsonObject idToken = JWT.parse(user.principal().getString("id_token"))
                        .getJsonObject("payload");
                    final String id = idToken.getString("sub");
                    final String email = idToken.getString("email");
                    final String hostedDomain = idToken.getString("hd");

                    logger.info(() -> "Google User: id: " + id + " email: " + email + " domain: " + hostedDomain + " logged in");
                    session.put("token", user.principal().getString("access_token"));
                    session.put("id", id);
                    session.put("email", email);
                    session.put("domain", hostedDomain);
                    response.setStatusCode(200);
                    response.end("\"Successfully connected user.\"");
                } else {
                    response.setStatusCode(500);
                    response.end("\"Failed to read token data from Google. "
                        + ar.cause().getMessage() + "\"");
                }
            });
        });
    }
}
