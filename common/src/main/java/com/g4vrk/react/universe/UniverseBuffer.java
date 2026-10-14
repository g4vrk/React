package com.g4vrk.react.universe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class UniverseBuffer {
    private final UniverseFrame[] frames;
    private int writeIndex;
    private int size;
    private int framesSinceLastSnapshot;
    private boolean firstSnapshotSent;

    public UniverseBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        this.frames = new UniverseFrame[capacity];
    }

    public synchronized void addFrame(UniverseFrame frame) {
        this.frames[this.writeIndex] = Objects.requireNonNull(frame, "frame");
        this.writeIndex = (this.writeIndex + 1) % this.frames.length;
        if (this.size < this.frames.length) {
            ++this.size;
        }
        ++this.framesSinceLastSnapshot;
    }

    public synchronized boolean isFull() {
        return this.size == this.frames.length;
    }

    public synchronized int size() {
        return this.size;
    }

    public int capacity() {
        return this.frames.length;
    }

    public synchronized List<UniverseFrame> pollSnapshot(int step) {
        int safeStep = Math.max(1, step);
        if (!this.isFull()) {
            return null;
        }
        if (this.firstSnapshotSent && this.framesSinceLastSnapshot < safeStep) {
            return null;
        }
        List<UniverseFrame> snapshot = this.createSnapshot();
        this.firstSnapshotSent = true;
        this.framesSinceLastSnapshot = 0;
        return snapshot;
    }

    public synchronized List<UniverseFrame> getSnapshot() {
        return this.createSnapshot();
    }

    private List<UniverseFrame> createSnapshot() {
        ArrayList<UniverseFrame> snapshot = new ArrayList<UniverseFrame>(this.size);
        int oldestIndex = this.size == this.frames.length ? this.writeIndex : 0;
        for (int i = 0; i < this.size; ++i) {
            int index = (oldestIndex + i) % this.frames.length;
            snapshot.add(this.frames[index]);
        }
        return snapshot;
    }

    public synchronized void clear() {
        Arrays.fill(this.frames, null);
        this.writeIndex = 0;
        this.size = 0;
        this.framesSinceLastSnapshot = 0;
        this.firstSnapshotSent = false;
    }
}
