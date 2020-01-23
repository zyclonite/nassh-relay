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

import io.vertx.core.http.HttpServerRequest;

public class RequestHelper {
    private RequestHelper() {
        //
    }

    public static String getHost(final HttpServerRequest request) {
        if (request.headers().contains("X-Forwarded-Host")) {
            return request.headers().get("X-Forwarded-Host");
        } else {
            return request.host();
        }
    }

    public static String getRemoteHost(final HttpServerRequest request) {
        if (request.headers().contains("X-Real-IP")) {
            return request.headers().get("X-Real-IP");
        } else {
            return request.remoteAddress().host();
        }
    }
}
