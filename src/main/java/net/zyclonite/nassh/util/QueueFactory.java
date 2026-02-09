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

import java.util.HashMap;
import java.util.Map;

/**
 * @author zyclonite
 */
public class QueueFactory {

    private final static Map<String, TransferQueue> queues = new HashMap<>();

    public static void createQueue(final String name) {
        queues.put(name, new TransferQueue());
    }

    public static TransferQueue getQueue(final String name) throws NoSuchQueueException {
        if (queues.containsKey(name)) {
            return queues.get(name);
        }
        throw new NoSuchQueueException(name + " not found");
    }

    public static void deleteQueue(final String name) {
        if (queues.containsKey(name)) {
            queues.remove(name).clear();
        }
    }
}
