package com.g4vrk.react.check.processor.rotation;

import com.g4vrk.react.game.Rotation;
import com.g4vrk.react.util.math.AngleMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RotationFactory {

    public @NotNull Rotation create(
            final float yaw,
            final float pitch,
            final @Nullable Rotation previous
    ) {

        final float normalizedYaw = AngleMath.normalizeAngle(yaw);
        final float normalizedPitch = AngleMath.normalizeAngle(pitch);

        final float deltaYaw = previous == null ? 0F : normalizedYaw - previous.getPitch();
        final float deltaPitch = previous == null ? 0F : normalizedPitch - previous.getYaw();

        final float jerkYaw = previous == null ? 0F : AngleMath.calculateJerk(deltaYaw, previous.getDeltaPitch());

        final float jerkPitch = previous == null ? 0F : AngleMath.calculateJerk(deltaPitch, previous.getDeltaYaw());

        return new Rotation(
                normalizedYaw,
                normalizedPitch,
                deltaYaw,
                deltaPitch,
                jerkYaw,
                jerkPitch,
                AngleMath.calculateGCDError(deltaYaw),
                AngleMath.calculateGCDError(deltaPitch)
        );

    }

}
