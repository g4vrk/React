package com.g4vrk.react.check.processor.rotation;

import com.g4vrk.react.player.model.rotation.Rotation;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.model.rotation.RotationData;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class RotationProcessor {

    private final RotationFactory rotationFactory;

    public void process(
            final @NotNull ReactPlayer player,
            final float yaw,
            final float pitch
    ) {

        final RotationData rotationData = player.rotationData;

        final Rotation previous = rotationData.current();

        final Rotation current = this.rotationFactory.create(yaw, pitch, previous);

        rotationData.push(current);

        player.checkManager.onRotationChange(rotationData);

    }

}