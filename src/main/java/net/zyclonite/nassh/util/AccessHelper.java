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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.zyclonite.nassh.model.AuthSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author zyclonite
 */
public class AccessHelper {

    private static Logger logger = LogManager.getLogger();

    public static boolean isHostAllowed(final JsonArray accesslist, final JsonArray whitelist, final JsonArray blacklist, final InetAddress address, final AuthSession authSession) {
        if (authSession != null) {
            final boolean access = accesslist.stream().map(l -> (JsonObject) l)
                .filter(item -> filterUser(item, authSession))
                .anyMatch(item -> checkAccess(item.getJsonArray("access"), address));
            if (access) {
                return true;
            }
        }
        if (checkAccess(whitelist, address)) {
            return true;
        }
        return checkBlock(blacklist, address);
    }

    private static boolean filterUser(final JsonObject listItem, final AuthSession authSession) {
        final String id = authSession.get("id");
        final String domain = authSession.get("domain");
        final String email = authSession.get("email");
        if (listItem.containsKey("id") && id != null && listItem.getString("id").equals(id)) {
            return true;
        }
        if (listItem.containsKey("email") && email != null && listItem.getString("email").equals(email)) {
            return true;
        }
        return listItem.containsKey("domain") && domain != null && listItem.getString("domain").equals(domain);
    }

    private static boolean checkAccess(final JsonArray list, final InetAddress address) {
        return list.stream().map(l -> (JsonObject) l)
            .anyMatch(entry -> {
                if (entry.containsKey("network")) {
                    return checkNetwork(entry.getString("network"), address);
                } else {
                    return checkHost(entry.getString("host"), address);
                }
            });
    }

    private static boolean checkBlock(final JsonArray list, final InetAddress address) {
        return list.stream().map(l -> (JsonObject) l)
            .noneMatch(entry -> {
                if (entry.containsKey("network")) {
                    return checkNetwork(entry.getString("network"), address);
                } else {
                    return checkHost(entry.getString("host"), address);
                }
            });
    }

    private static boolean checkHost(final String block, final InetAddress address) {
        try {
            final InetAddress blk = InetAddress.getByName(block);
            if (address.getHostAddress().equals(blk.getHostAddress())) {
                return true;
            }
        } catch (final UnknownHostException ex) {
            logger.warn(() -> "Configuration error at " + block + " " + ex);
        }
        return false;
    }

    private static boolean checkNetwork(final String block, final InetAddress address) {
        try {
            final NetworkHelper netblk = new NetworkHelper(block);
            if (netblk.isInRange(address)) {
                return true;
            }
        } catch (final UnknownHostException ex) {
            logger.warn(() -> "Configuration error at " + block + " " + ex);
        }
        return false;
    }
}
