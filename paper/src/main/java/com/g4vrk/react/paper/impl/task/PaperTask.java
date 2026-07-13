package com.g4vrk.react.paper.impl.task;

import com.g4vrk.react.api.task.Task;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class PaperTask implements Task {

    private final BukkitTask task;

    public PaperTask(
            @NotNull BukkitTask task
    ) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }
}