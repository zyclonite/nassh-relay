/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2020   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.util;

import io.vertx.ext.web.RoutingContext;
import net.zyclonite.nassh.model.AuthSession;

import java.util.UUID;

/**
 * @author zyclonite
 */
public class WebHelper {

    public static AuthSession validateCookie(final RoutingContext context) {
        var cookie = context.request().getCookie(Constants.SESSIONCOOKIE);
        if (cookie == null) {
            return null;
        }
        var sessioncookie = UUID.fromString(cookie.getValue());
        var session = AuthSessionManager.getSession(sessioncookie);
        if (session == null) {
            return null;
        }
        var id = session.get("id");
        if (id != null) {
            return session;
        }
        return null;
    }
}
