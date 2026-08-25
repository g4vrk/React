package com.g4vrk.react.player.model.rotation;

import com.g4vrk.react.buffer.Buffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RotationData {

    private Rotation current;
    private Rotation previous;

    private Buffer<Rotation> history;

    public RotationData(
            int bufferSize
    ) {
        this.history = new Buffer<>(bufferSize, Rotation[]::new);
    }

    public synchronized void push(
            final @NotNull Rotation rotation
    ) {

        this.previous = this.current;
        this.current = rotation;

        this.history.add(rotation);

    }

    public synchronized @Nullable Rotation current() {
        return current;
    }

    public synchronized @Nullable Rotation previous() {
        return previous;
    }

    public synchronized @NotNull Rotation[] snapshotHistory() {
        return history.snapshot();
    }

    public synchronized int historySize() {
        return history.size();
    }

    public synchronized int historyCapacity() {
        return history.capacity();
    }

    public synchronized @Nullable Rotation[] drainHistory(
            final int minimumSize
    ) {
        if (minimumSize <= 0) {
            throw new IllegalArgumentException("minimumSize must be higher than 0");
        }

        if (history.size() < minimumSize) {
            return null;
        }

        final Rotation[] snapshot = history.snapshot();

        history.clear();

        return snapshot;
    }

    public synchronized void resizeHistory(
            final int bufferSize
    ) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be higher than 0");
        }

        if (history.capacity() == bufferSize) {
            return;
        }

        final Rotation[] snapshot = history.snapshot();
        final Buffer<Rotation> resizedHistory = new Buffer<>(bufferSize, Rotation[]::new);

        final int firstEntry = Math.max(0, snapshot.length - bufferSize);

        for (int i = firstEntry; i < snapshot.length; i++) {
            resizedHistory.add(snapshot[i]);
        }

        this.history = resizedHistory;
    }

    public synchronized void clear() {

        this.history.clear();

        this.current = null;
        this.previous = null;

    }

}
