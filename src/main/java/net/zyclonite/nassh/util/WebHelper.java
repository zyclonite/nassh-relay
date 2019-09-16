/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2018   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.util;

import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.AuthSession;

import java.util.UUID;

/**
 * @author zyclonite
 */
public class WebHelper {

    public static AuthSession validateCookie(final RoutingContext context) {
        final Cookie cookie = context.getCookie(Constants.SESSIONCOOKIE);
        if (cookie == null) {
            return null;
        }
        final UUID sessioncookie = UUID.fromString(cookie.getValue());
        final AuthSession session = AuthSessionManager.getSession(sessioncookie);
        if (session == null) {
            return null;
        }
        final String id = session.get("id");
        if (id != null) {
            return session;
        }
        return null;
    }
}
