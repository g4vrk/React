package com.g4vrk.react.folia.impl;

import com.g4vrk.react.api.ReactAPI;
import com.g4vrk.react.api.task.runner.factory.TaskRunnerFactory;
import com.g4vrk.react.folia.impl.task.runner.factory.FoliaTaskRunnerFactory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class FoliaReactAPI implements ReactAPI {

    private final TaskRunnerFactory taskRunnerFactory;

    public FoliaReactAPI(
            @NotNull Plugin plugin
    ) {
        this.taskRunnerFactory = new FoliaTaskRunnerFactory(plugin);
    }

    @Override
    public @NotNull TaskRunnerFactory getTaskRunnerFactory() {
        return taskRunnerFactory;
    }
}
