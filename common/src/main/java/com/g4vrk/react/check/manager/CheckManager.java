package com.g4vrk.react.check.manager;

import com.g4vrk.react.check.impl.aim.AimAI;
import com.g4vrk.react.check.type.RotationCheck;
import com.g4vrk.react.player.model.ReactPlayer;
import com.g4vrk.react.player.model.rotation.RotationData;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// realization like in GrimAC
public final class CheckManager {

    private final ClassToInstanceMap<RotationCheck> rotationChecks;

    private final List<RotationCheck> rotationChecksValues;

    public CheckManager(
            @NotNull ReactPlayer player
    ) {

        this.rotationChecks = new ImmutableClassToInstanceMap.Builder<RotationCheck>()
                .put(AimAI.class, new AimAI(player))
                .build();

        this.rotationChecksValues = new ObjectArrayList<>(this.rotationChecks.values());

    }

    public @Nullable RotationCheck getRotationCheck(
            final @NotNull Class<? extends RotationCheck> checkClass
    ) {
        return this.rotationChecks.get(checkClass);
    }

    public void onRotationChange(
            final @NotNull RotationData data
    ) {

        for (final RotationCheck check : rotationChecksValues) {
            check.onRotation(data);
        }

    }

}