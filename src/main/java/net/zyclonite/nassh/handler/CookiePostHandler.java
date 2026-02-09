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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.auth.oauth2.providers.GoogleAuth;
import io.vertx.ext.web.RoutingContext;
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
        var auth = config.getJsonObject("auth");
        this.oauth2 = GoogleAuth.create(vertx, auth.getString("client-id"), auth.getString("client-secret"));
    }

    @Override
    public void handle(final RoutingContext context) {
        logger.debug(() -> "got request");
        var request = context.request();
        var response = context.response();
        response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.putHeader("Pragma", "no-cache");
        response.putHeader("Content-Type", "no-cache");
        response.putHeader("Content-Type", "application/json");
        var cookie = context.request().getCookie(Constants.SESSIONCOOKIE);
        UUID sessioncookie;
        if (cookie == null) {
            sessioncookie = null;
        } else {
            sessioncookie = UUID.fromString(cookie.getValue());
        }
        var session = AuthSessionManager.getSession(sessioncookie);
        if (session == null) {
            response.setStatusCode(403);
            response.end("\"Invalid session cookie.\"");
            return;
        }
        var token = session.get("token");
        var state = session.get("state");
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
            var oauth2Credentials = new Oauth2Credentials()
                .setCode(body.toString())
                .setRedirectUri("postmessage");
            oauth2.authenticate(oauth2Credentials).andThen(ar -> {
                if (ar.succeeded() && ar.result() != null) {
                    var user = ar.result();
                    var idToken = JWT.parse(user.principal().getString("id_token"))
                        .getJsonObject("payload");
                    var id = idToken.getString("sub");
                    var email = idToken.getString("email");
                    var hostedDomain = idToken.getString("hd");

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
