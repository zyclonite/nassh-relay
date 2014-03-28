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

import org.vertx.java.core.shareddata.Shareable;

/**
 *
 * @author zyclonite
 */
public class Session implements Shareable {
    private String handler;
    private int write_count;
    private int read_count;
    private boolean active = true;

    /**
     * @return the handler
     */
    public String getHandler() {
        return handler;
    }

    /**
     * @param handler the handler to set
     */
    public void setHandler(String handler) {
        this.handler = handler;
    }

    /**
     * @return the write_count
     */
    public int getWrite_count() {
        return write_count;
    }

    /**
     * @param write_count the write_count to set
     */
    public void setWrite_count(int write_count) {
        this.write_count = write_count;
    }

    /**
     * @return the read_count
     */
    public int getRead_count() {
        return read_count;
    }

    /**
     * @param read_count the read_count to set
     */
    public void setRead_count(int read_count) {
        this.read_count = read_count;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}
