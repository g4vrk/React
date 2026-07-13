package com.g4vrk.react.paper.impl;

import com.g4vrk.react.api.ReactAPI;
import com.g4vrk.react.api.task.runner.factory.TaskRunnerFactory;
import com.g4vrk.react.paper.impl.task.runner.factory.PaperTaskRunnerFactory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class PaperReactAPI implements ReactAPI {

    private final TaskRunnerFactory taskRunnerFactory;

    public PaperReactAPI(
            @NotNull Plugin plugin
    ) {
        this.taskRunnerFactory = new PaperTaskRunnerFactory(plugin);
    }

    @Override
    public @NotNull TaskRunnerFactory getTaskRunnerFactory() {
        return taskRunnerFactory;
    }
}
