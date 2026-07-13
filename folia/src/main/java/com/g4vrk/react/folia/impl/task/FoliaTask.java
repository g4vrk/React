package com.g4vrk.react.folia.impl.task;

import com.g4vrk.react.api.task.Task;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;

public final class FoliaTask implements Task {

    private final ScheduledTask task;

    public FoliaTask(
            @NotNull ScheduledTask task
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