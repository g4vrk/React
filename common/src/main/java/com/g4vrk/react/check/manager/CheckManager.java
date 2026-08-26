package com.g4vrk.react.check.manager;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.check.Check;
import com.g4vrk.react.check.impl.aim.AimAI;
import com.g4vrk.react.check.type.RotationCheck;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.model.rotation.RotationData;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

// realization like in GrimAC
public final class CheckManager {

    private final ClassToInstanceMap<RotationCheck> rotationChecks;

    private final List<RotationCheck> rotationChecksValues;

    private final List<Check> checksValues;

    public CheckManager(
            @NotNull ReactPlayer player
    ) {

        this.rotationChecks = new ImmutableClassToInstanceMap.Builder<RotationCheck>()
                .put(AimAI.class, new AimAI(player))
                .build();

        this.rotationChecksValues = new ObjectArrayList<>(this.rotationChecks.values());
        this.checksValues = this.rotationChecksValues.stream()
                .map(check -> (Check) check)
                .toList();

        this.reload();

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

    public @NotNull List<Check> checks() {
        return this.checksValues;
    }

    public @Nullable Check getCheck(final @NotNull String configId) {
        for (final Check check : checksValues) {
            if (check.getConfigId().equalsIgnoreCase(configId)) {
                return check;
            }
        }
        return null;
    }

    public void reload() {

        for (final Map.Entry<Class<? extends RotationCheck>, RotationCheck> entry : rotationChecks.entrySet()) {

            final Config config = React.INSTANCE.getCheckConfigRegistry()
                    .config(entry.getKey());

            if (entry.getValue() instanceof final ReloadObserver reloadableCheck) {

                reloadableCheck.onReload(config);

            }

        }

    }

}
