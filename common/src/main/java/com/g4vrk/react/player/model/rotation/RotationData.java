package com.g4vrk.react.player.model.rotation;

import com.g4vrk.react.buffer.Buffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RotationData {

    private Rotation current;
    private Rotation previous;

    private final Buffer<Rotation> history;

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

    public @NotNull Rotation[] snapshotHistory() {
        return history.snapshot();
    }

    public int historySize() {
        return history.size();
    }

    public synchronized void clear() {

        this.history.clear();

        this.current = null;
        this.previous = null;

    }

}