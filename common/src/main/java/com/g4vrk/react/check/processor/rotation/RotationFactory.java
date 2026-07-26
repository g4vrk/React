package com.g4vrk.react.check.processor.rotation;

import com.g4vrk.react.player.model.rotation.Rotation;
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

        final float deltaYaw = previous == null ? 0F : AngleMath.normalizeDelta(normalizedYaw - previous.getX());
        final float deltaPitch = previous == null ? 0F : AngleMath.normalizeDelta(normalizedPitch - previous.getY());

        final float accelYaw = previous == null ? 0F : AngleMath.calculateAcceleration(deltaYaw, previous.getDeltaX());
        final float accelPitch = previous == null ? 0F : AngleMath.calculateAcceleration(deltaPitch, previous.getDeltaY());

        return new Rotation(
                normalizedPitch,
                normalizedYaw,
                deltaPitch,
                deltaYaw,
                accelPitch,
                accelYaw,
                AngleMath.calculateGCDError(deltaPitch),
                AngleMath.calculateGCDError(deltaYaw)
        );

    }

}
