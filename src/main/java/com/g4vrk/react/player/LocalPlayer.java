package com.g4vrk.react.player;

import com.g4vrk.react.buffer.Buffer;
import com.g4vrk.react.game.Rotation;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public final class LocalPlayer {

    private final UUID uniqueId;
    private final String name;

    private final PlayerActivity activity = new PlayerActivity();
    private final RotationState movement = new RotationState();

    private final Buffer<Rotation> rotations;

    public LocalPlayer(
            @NotNull UUID uniqueId,
            @NotNull String name,
            int bufferSize
    ) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.rotations = new Buffer<>(bufferSize);
    }

    public void addRotation(
            final @NotNull Rotation rotation
    ) {
        rotations.add(rotation);
    }

    public @NotNull Buffer<Rotation> getBuffer() {
        return rotations;
    }

    public @NotNull Rotation @NotNull [] snapshotRotations() {
        return rotations.snapshot();
    }

    public void clearRotations() {
        rotations.clear();
    }
}