/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2016   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.model;

import java.util.Date;
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
    private final int ttl;
    private long lastAccessed;

    public AuthSession(final int ttl) {
        this.id = UUID.randomUUID();
        this.ttl = ttl;
        this.lastAccessed = new Date().getTime();
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

    public boolean isValid(final long now) {
        return ((lastAccessed + (ttl * 1000)) < now);
    }

    public void refresh() {
        lastAccessed = new Date().getTime();
    }
}
