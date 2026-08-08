package com.g4vrk.react.buffer;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.IntFunction;

public final class Buffer<T> implements Iterable<T> {

    private final Object[] buffer;
    private final IntFunction<T[]> arrayFactory;
    private final int capacity;

    private int head = 0;
    private int size = 0;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Buffer(int capacity, @NotNull IntFunction<T[]> arrayFactory) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be higher than 0");
        }

        this.capacity = capacity;
        this.buffer = new Object[capacity];
        this.arrayFactory = arrayFactory;
    }

    public void add(T value) {
        lock.writeLock().lock();
        try {
            buffer[head] = value;

            head = (head + 1) % capacity;

            if (size < capacity) {
                size++;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T[] snapshot() {
        lock.readLock().lock();
        try {
            T[] out = arrayFactory.apply(size);

            int start = (head - size + capacity) % capacity;

            for (int i = 0; i < size; i++) {
                out[i] = (T) buffer[(start + i) % capacity];
            }

            return out;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            Arrays.fill(buffer, null);
            head = 0;
            size = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        final Object[] snap = snapshot();

        return new Iterator<>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < snap.length;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                if (index >= snap.length) throw new NoSuchElementException();
                return (T) snap[index++];
            }
        };
    }

    @SuppressWarnings("unchecked")
    public T getUnsafe(int index) {
        lock.readLock().lock();
        try {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException();

            int start = (head - size + capacity) % capacity;
            return (T) buffer[(start + index) % capacity];
        } finally {
            lock.readLock().unlock();
        }
    }
}