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

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import net.zyclonite.nassh.relay.model.AuthSession;
import net.zyclonite.nassh.relay.util.AuthSessionManager;
import net.zyclonite.nassh.relay.util.Constants;
import net.zyclonite.nassh.relay.verticle.WebService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

/**
 *
 * @author zyclonite
 */
public class CookiePostHandler implements Handler<HttpServerRequest> {

    private static final Log LOG = LogFactory.getLog(CookiePostHandler.class);

    @Override
    public void handle(final HttpServerRequest request) {
        LOG.debug("got request");
        request.response().putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        request.response().putHeader("Pragma", "no-cache");
        request.response().putHeader("Content-Type", "no-cache");
        request.response().putHeader("Content-Type", "application/json");
        final String cookie_header = request.headers().get("Cookie");
        final Set<Cookie> cookies = CookieDecoder.decode(cookie_header);
        UUID sessioncookie = null;
        for (final Cookie cookie : cookies) {
            if (Constants.SESSIONCOOKIE.equals(cookie.getName())) {
                sessioncookie = UUID.fromString(cookie.getValue());
                break;
            }
        }
        final AuthSession session = AuthSessionManager.getSession(sessioncookie);
        if (session == null) {
            request.response().setStatusCode(403);
            request.response().end(WebService.GSON.toJson("Invalid session cookie."));
            return;
        }
        final String token = session.get("token");
        final String state = session.get("state");
        if (token != null) {
            request.response().setStatusCode(200);
            request.response().end(WebService.GSON.toJson("Current user is already connected."));
            return;
        }
        if (!request.params().contains("state") || !request.params().get("state").equals(state)) {
            request.response().setStatusCode(403);
            request.response().end(WebService.GSON.toJson("Invalid state parameter."));
            return;
        }
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(final Buffer body) {
                try {
                    final GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(WebService.TRANSPORT, WebService.JSON_FACTORY,
                            WebService.CLIENT_ID, WebService.CLIENT_SECRET, body.toString("UTF-8"), "postmessage").execute();

                    final GoogleIdToken idToken = tokenResponse.parseIdToken();
                    final String gplusid = idToken.getPayload().getSubject();

                    LOG.info("Google Plus user: " + gplusid + " logged in");
                    session.put("token", tokenResponse.toString());
                    session.put("gplusid", gplusid);
                    request.response().setStatusCode(200);
                    request.response().end(WebService.GSON.toJson("Successfully connected user."));
                } catch (TokenResponseException e) {
                    request.response().setStatusCode(500);
                    request.response().end(WebService.GSON.toJson("Failed to upgrade the authorization code."));
                } catch (IOException e) {
                    request.response().setStatusCode(500);
                    request.response().end(WebService.GSON.toJson("Failed to read token data from Google. "
                            + e.getMessage()));
                }
            }
        });
    }
}
