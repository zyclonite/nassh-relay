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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.zyclonite.nassh.relay.model.AuthSession;

/**
 *
 * @author zyclonite
 */
public class AuthSessionManager {

    private final static Map<UUID, AuthSession> STORE = new HashMap<>();
    private final static Map<UUID, Date> TIME = new HashMap<>();
    private static int ttl = 600; //default 10 min
    private static long lastcheck = (new Date()).getTime();

    public static void setTTL(final int ttl_new) {
        ttl = ttl_new;
    }

    public static AuthSession createSession() {
        checkExpiration();
        final AuthSession session = new AuthSession();
        TIME.put(session.getId(), new Date());
        STORE.put(session.getId(), session);
        return session;
    }

    public static AuthSession getSession(final UUID id) {
        checkExpiration();
        if (STORE.containsKey(id)) {
            return STORE.get(id);
        } else {
            return null;
        }
    }
    
    public static void removeSession(final UUID id) {
        if(TIME.containsKey(id)){
            if(STORE.containsKey(id)) {
                STORE.remove(id);
            }
            TIME.remove(id);
        }
    }

    private static void checkExpiration() {
        final long now = (new Date()).getTime();
        if(lastcheck+(10*1000) < now){
            return;
        }
        for(final Entry<UUID, Date> set:TIME.entrySet()) {
            if((set.getValue().getTime()+(ttl*1000)) < now){
                removeSession(set.getKey());
            }
        }
        lastcheck = now;
    }
}
