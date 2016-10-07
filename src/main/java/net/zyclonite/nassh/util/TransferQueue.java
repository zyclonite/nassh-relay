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

import io.vertx.core.buffer.Buffer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Observable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author zyclonite
 */
public class TransferQueue extends Observable implements Queue<Buffer> {

    private final ConcurrentLinkedQueue<Buffer> queue = new ConcurrentLinkedQueue<>();

    @Override
    public Buffer poll() {
        return queue.poll();
    }

    @Override
    public boolean add(Buffer buffer) {
        if (queue.add(buffer)) {
            setChanged();
            notifyObservers();
            return true;
        }
        return false;
    }

    public boolean isFull() {
        if(queue.size() >= Constants.QUEUEMAXSIZE) {
            return true;
        }
        return false;
    }

    @Override
    public boolean offer(Buffer e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Buffer remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Buffer element() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Buffer peek() {
        return queue.peek();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<Buffer> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean addAll(Collection<? extends Buffer> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        queue.clear();
    }
}
