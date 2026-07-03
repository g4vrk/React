package com.g4vrk.react.ml.check;

import com.g4vrk.react.game.Rotation;
import com.g4vrk.react.ml.check.processor.MLCheckProcessor;
import com.g4vrk.react.player.model.LocalPlayer;
import com.g4vrk.react.player.CombatActivity;
import com.g4vrk.react.runner.TaskRunner;
import org.jetbrains.annotations.NotNull;

public final class MLCheck {

    private final TaskRunner taskRunner;
    private final MLCheckProcessor mlCheckProcessor;
    private final int threshold;

    public MLCheck(
            @NotNull TaskRunner taskRunner,
            @NotNull MLCheckProcessor mlCheckProcessor,
            int threshold
    ) {
        this.taskRunner = taskRunner;
        this.mlCheckProcessor = mlCheckProcessor;
        this.threshold = threshold;
    }

    public void onRotation(
            final @NotNull LocalPlayer player,
            final @NotNull CombatActivity combatActivity,
            final @NotNull Rotation rotation
    ) {
        if (!combatActivity.isActive()) return;

        player.addRotation(rotation);

        if (player.getBuffer().size() < threshold) return;

        taskRunner.runTaskAsynchronously(() -> {
            mlCheckProcessor.check(player);
            player.clearRotations();
            combatActivity.clear();
        });
    }
}