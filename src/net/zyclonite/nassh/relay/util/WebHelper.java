/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.util;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import java.util.Set;
import java.util.UUID;
import net.zyclonite.nassh.relay.model.AuthSession;
import org.vertx.java.core.http.HttpServerRequest;

/**
 *
 * @author zyclonite
 */
public class WebHelper {

    public static String validateCookie(final HttpServerRequest request) {
        if (!request.headers().contains("Cookie")) {
            return null;
        }
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
            return null;
        }
        final String gplusid = session.get("gplusid");
        if (gplusid != null) {
            return gplusid;
        }
        return null;
    }

    public static void putAccessControlAllowHeader(final HttpServerRequest request) {
        if (!request.headers().contains("Origin")) {
            return;
        }
        final String reqorigin = request.headers().get("Origin");
        for (final String origin : Constants.ORIGINS) {
            if (origin.equals(reqorigin)) {
                request.response().putHeader("Access-Control-Allow-Origin", reqorigin);
                return;
            }
        }
    }
}
