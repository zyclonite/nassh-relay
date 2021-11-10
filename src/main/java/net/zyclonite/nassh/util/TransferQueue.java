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

import io.vertx.core.buffer.Buffer;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author zyclonite
 */
public class TransferQueue implements Queue<Buffer> {
    private boolean changed = false;
    private final Vector<TransferObserver> obs;
    private final ConcurrentLinkedQueue<Buffer> queue = new ConcurrentLinkedQueue<>();

    public TransferQueue() {
        obs = new Vector<>();
    }

    public synchronized void addObserver(final TransferObserver o) {
        if (o == null) {
            throw new NullPointerException();
        }
        if (!obs.contains(o)) {
            obs.addElement(o);
        }
    }

    protected synchronized void deleteObserver(final TransferObserver o) {
        obs.removeElement(o);
    }

    public synchronized void deleteObservers() {
        obs.removeAllElements();
    }

    public synchronized int countObservers() {
        return obs.size();
    }

    private synchronized void setChanged() {
        changed = true;
    }

    private synchronized void clearChanged() {
        changed = false;
    }

    private void notifyObservers() {
        final TransferObserver[] arrLocal;

        synchronized (this) {
            if (!changed) {
                return;
            }
            arrLocal = obs.toArray(new TransferObserver[0]);
            clearChanged();
        }

        for (int i = arrLocal.length - 1; i >= 0; i--) {
            arrLocal[i].update(this);
        }
    }

    public boolean isFull() {
        return queue.size() >= Constants.QUEUEMAXSIZE;
    }

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

    @Override
    public Buffer peek() {
        return queue.peek();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public void clear() {
        queue.clear();
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
}
