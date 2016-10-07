/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2016   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.util;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.AuthSession;

import java.util.UUID;

/**
 *
 * @author zyclonite
 */
public class WebHelper {

    public static String validateCookie(final RoutingContext context) {
        final Cookie cookie = context.getCookie(Constants.SESSIONCOOKIE);
        if(cookie == null) {
            return null;
        }
        final UUID sessioncookie = UUID.fromString(cookie.getValue());
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
