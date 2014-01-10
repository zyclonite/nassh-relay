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

/**
 *
 * @author zyclonite
 */
public class NoSuchQueueException extends Exception {

    public NoSuchQueueException(String message) {
        super(message);
    }
}
