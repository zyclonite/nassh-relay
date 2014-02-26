/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author zyclonite
 */
public class AuthSession {

    private final UUID id;
    private final Map<String, String> keyvalues = new HashMap<>();

    public AuthSession() {
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public String get(final String key) {
        if (keyvalues.containsKey(key)) {
            return keyvalues.get(key);
        } else {
            return null;
        }
    }

    public void put(final String key, final String value) {
        keyvalues.put(key, value);
    }
}
