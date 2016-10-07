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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.zyclonite.nassh.model.AuthSession;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author zyclonite
 */
public class AccessHelper {

    private static Logger logger = LoggerFactory.getLogger(AccessHelper.class);

    public static boolean isHostAllowed(final JsonArray accesslist, final JsonArray blacklist, final InetAddress address, final AuthSession authSession) {
        if(isHostAllowed(blacklist, address)) {
            return true;
        }
        if (authSession != null) {
            return accesslist.stream().map(l -> (JsonObject)l)
                    .filter(item -> filterUser(item, authSession))
                    .anyMatch(item -> checkAccess(item.getJsonArray("access"), address, false));
        }
        return false;
    }

    public static boolean isHostAllowed(final JsonArray blacklist, final InetAddress address) {
        return checkAccess(blacklist, address, true);
    }

    private static boolean filterUser(final JsonObject listItem, final AuthSession authSession) {
        final String id = authSession.get("id");
        final String domain = authSession.get("domain");
        final String email = authSession.get("email");
        if(listItem.containsKey("id") && id != null && listItem.getString("id").equals(id)) {
            return true;
        }
        if(listItem.containsKey("email") && email != null && listItem.getString("email").equals(email)) {
            return true;
        }
        if(listItem.containsKey("domain") && domain != null && listItem.getString("domain").equals(domain)) {
            return true;
        }
        return false;
    }

    private static boolean checkAccess(final JsonArray list, final InetAddress address, final boolean blacklist) {
        return list.stream().map(l -> (JsonObject)l)
                .anyMatch(entry -> {
                    if(entry.containsKey("network")) {
                        return checkNetwork(entry.getString("network"), address, blacklist);
                    } else {
                        return checkHost(entry.getString("host"), address, blacklist);
                    }
                });
    }

    private static boolean checkHost(final String block, final InetAddress address, final boolean blacklist) {
        try {
            final InetAddress blk = InetAddress.getByName(block);
            if (address.getHostAddress().equals(blk.getHostAddress())) {
                return !blacklist;
            }
        } catch (final UnknownHostException ex) {
            logger.warn("Configuration error at " + block + " " + ex);
        }
        return blacklist;
    }

    private static boolean checkNetwork(final String block, final InetAddress address, final boolean blacklist) {
        try {
            final NetworkHelper netblk = new NetworkHelper(block);
            if (netblk.isInRange(address)) {
                return !blacklist;
            }
        } catch (final UnknownHostException ex) {
            logger.warn("Configuration error at " + block + " " + ex);
        }
        return blacklist;
    }
}
