/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2016   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.util;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zyclonite
 */
public class QueueFactory {

    private final static Map<String, TransferQueue> queues = new HashMap<>();

    public static TransferQueue createQueue(final String name) {
            final TransferQueue queue = new TransferQueue();
            queues.put(name, queue);
            return queue;
    }
    
    public static TransferQueue getQueue(final String name) throws NoSuchQueueException {
        if (queues.containsKey(name)) {
            return queues.get(name);
        }
        throw new NoSuchQueueException(name+" not found");
    }

    public static void deleteQueue(final String name) {
        if (queues.containsKey(name)) {
            queues.remove(name).clear();
        }
    }
}
