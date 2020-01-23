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

/**
 * @author zyclonite
 */
public class NoSuchQueueException extends Exception {

    public NoSuchQueueException(String message) {
        super(message);
    }
}
