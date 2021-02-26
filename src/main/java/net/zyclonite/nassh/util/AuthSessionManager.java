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

import net.zyclonite.nassh.model.AuthSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zyclonite
 */
public class AuthSessionManager {

    private static Logger logger = LogManager.getLogger();
    private final static Map<UUID, AuthSession> STORE = new ConcurrentHashMap<>();
    private static long lastcheck = (new Date()).getTime();

    public static AuthSession createSession(final int ttl) {
        checkExpiration();
        final AuthSession session = new AuthSession(ttl);
        STORE.put(session.getId(), session);
        return session;
    }

    public static AuthSession getSession(final UUID id) {
        checkExpiration();
        if ((id != null) && (STORE.containsKey(id))) {
            final AuthSession session = STORE.get(id);
            session.refresh();
            return session;
        } else {
            return null;
        }
    }

    public static void removeSession(final UUID id) {
        STORE.remove(id);
    }

    private static void checkExpiration() {
        final long now = (new Date()).getTime();
        logger.debug(() -> now + " " + lastcheck);
        if (lastcheck + (10 * 1000) > now) {
            return;
        }
        for (final Entry<UUID, AuthSession> set : STORE.entrySet()) {
            if (set.getValue().isValid(now)) {
                final UUID key = set.getKey();
                logger.debug(() -> "Removed session " + key);
                removeSession(key);
            }
        }
        lastcheck = now;
    }
}
