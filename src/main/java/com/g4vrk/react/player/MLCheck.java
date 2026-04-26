package com.g4vrk.react.player;

import com.g4vrk.react.game.Rotation;
import com.g4vrk.react.runner.TaskRunner;
import com.g4vrk.react.ml.check.MLCheckProcessor;
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

    public void onFrame(
            final @NotNull LocalPlayer player,
            final @NotNull PlayerActivity activity,
            final @NotNull Rotation rotation
    ) {
        if (!activity.isActive()) return;

        player.addRotation(rotation);

        if (player.getBuffer().size() < threshold) return;

        taskRunner.runTaskAsynchronously(() -> {
            mlCheckProcessor.check(player);
            player.clearRotation();
            activity.clear();
        });
    }
}