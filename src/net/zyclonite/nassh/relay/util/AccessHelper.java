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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author zyclonite
 */
public class AccessHelper {

    private static final Log LOG = LogFactory.getLog(AccessHelper.class);
    private static final AppConfig CONFIG = AppConfig.getInstance();

    public static boolean isHostAllowed(final InetAddress address, final String userid) {
        if (userid != null) {
            final Object users = CONFIG.getProperty("accesslist.user.id");
            if (users instanceof Collection) {
                final int size = ((Collection) users).size();
                for (int i = 0; i < size; i++) {
                    if (userid.equals(CONFIG.getString("accesslist.user(" + i + ").id"))) {
                        if (checkBlockNetwork("accesslist.user(" + i + ").network", address, false)) {
                            return true;
                        }
                        if (checkBlockHost("accesslist.user(" + i + ").host", address, false)) {
                            return true;
                        }
                    }
                }
            } else if (users instanceof String) {
                if (userid.equals(CONFIG.getString("accesslist.user.id"))) {
                    if (checkBlockNetwork("accesslist.user.network", address, false)) {
                        return true;
                    }
                    if (checkBlockHost("accesslist.user.host", address, false)) {
                        return true;
                    }
                }
            }
        }
        return isHostAllowed(address);
    }

    public static boolean isHostAllowed(final InetAddress address) {
        if (checkBlockNetwork("application.blacklist.network", address, true)) {
            return checkBlockHost("application.blacklist.host", address, true);
        } else {
            return false;
        }
    }

    private static boolean checkBlockHost(final String block, final InetAddress address, final boolean blacklist) {
        final Object entries = CONFIG.getProperty(block);
        if (entries instanceof Collection) {
            final int size = ((Collection) entries).size();
            for (int i = 0; i < size; i++) {
                try {
                    final InetAddress blk = InetAddress.getByName(CONFIG.getString(block + "(" + i + ")"));
                    if (address.getHostAddress().equals(blk.getHostAddress())) {
                        return !blacklist;
                    }
                } catch (UnknownHostException ex) {
                    LOG.warn("Configuration error at " + block + "(" + i + ") " + ex);
                }
            }
        } else if (entries instanceof String) {
            try {
                final InetAddress blk = InetAddress.getByName(CONFIG.getString(block));
                if (address.getHostAddress().equals(blk.getHostAddress())) {
                    return !blacklist;
                }
            } catch (UnknownHostException ex) {
                LOG.warn("Configuration error at " + block + " " + ex);
            }
        }
        return blacklist;
    }

    private static boolean checkBlockNetwork(final String block, final InetAddress address, final boolean blacklist) {
        final Object entries = CONFIG.getProperty(block);
        if (entries instanceof Collection) {
            final int size = ((Collection) entries).size();
            for (int i = 0; i < size; i++) {
                try {
                    final NetworkHelper netblk = new NetworkHelper(CONFIG.getString(block + "(" + i + ")"));
                    if (netblk.isInRange(address)) {
                        return !blacklist;
                    }
                } catch (UnknownHostException ex) {
                    LOG.warn("Configuration error at " + block + "(" + i + ") " + ex);
                }
            }
        } else if (entries instanceof String) {
            try {
                final NetworkHelper netblk = new NetworkHelper(CONFIG.getString(block));
                if (netblk.isInRange(address)) {
                    return !blacklist;
                }
            } catch (UnknownHostException ex) {
                LOG.warn("Configuration error at " + block + " " + ex);
            }
        }
        return blacklist;
    }
}
