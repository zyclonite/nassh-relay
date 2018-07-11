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

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.AuthSession;
import net.zyclonite.nassh.util.AuthSessionManager;
import net.zyclonite.nassh.util.Constants;

import java.io.IOException;
import java.util.UUID;

/**
 * @author zyclonite
 */
public class CookiePostHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(CookiePostHandler.class);
    private final JsonObject auth;
    private final JsonFactory jsonFactory;
    private final HttpTransport httpTransport;

    public CookiePostHandler(final JsonObject config) {
        this.auth = config.getJsonObject("auth");
        this.jsonFactory = new JacksonFactory();
        this.httpTransport = new NetHttpTransport();
    }

    @Override
    public void handle(final RoutingContext context) {
        logger.debug("got request");
        final HttpServerRequest request = context.request();
        request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        request.response().putHeader("Pragma", "no-cache");
        request.response().putHeader("Content-Type", "no-cache");
        request.response().putHeader("Content-Type", "application/json");
        final Cookie cookie = context.getCookie(Constants.SESSIONCOOKIE);
        UUID sessioncookie;
        if (cookie == null) {
            sessioncookie = null;
        } else {
            sessioncookie = UUID.fromString(cookie.getValue());
        }
        final AuthSession session = AuthSessionManager.getSession(sessioncookie);
        if (session == null) {
            request.response().setStatusCode(403);
            request.response().end("\"Invalid session cookie.\"");
            return;
        }
        final String token = session.get("token");
        final String state = session.get("state");
        if (token != null) {
            request.response().setStatusCode(200);
            request.response().end("\"Current user is already connected.\"");
            return;
        }
        if (!request.params().contains("state") || !request.params().get("state").equals(state)) {
            request.response().setStatusCode(403);
            request.response().end("\"Invalid state parameter.\"");
            return;
        }
        request.bodyHandler(body -> {
            try {
                final GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(httpTransport, jsonFactory,
                    auth.getString("client-id"), auth.getString("client-secret"), body.toString(), "postmessage").execute();

                final GoogleIdToken idToken = tokenResponse.parseIdToken();
                final String gplusid = idToken.getPayload().getSubject();
                final String email = idToken.getPayload().getEmail();
                final String hostedDomain = idToken.getPayload().getHostedDomain();

                logger.info("Google Plus user: id: " + gplusid + " email: " + email + " domain: " + hostedDomain + " logged in");
                session.put("token", tokenResponse.toString());
                session.put("id", gplusid);
                session.put("email", email);
                session.put("domain", hostedDomain);
                request.response().setStatusCode(200);
                request.response().end("\"Successfully connected user.\"");
            } catch (TokenResponseException e) {
                request.response().setStatusCode(500);
                request.response().end("\"Failed to upgrade the authorization code.\"");
            } catch (IOException e) {
                request.response().setStatusCode(500);
                request.response().end("\"Failed to read token data from Google. "
                    + e.getMessage() + "\"");
            }
        });
    }
}
