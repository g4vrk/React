package com.g4vrk.react.runner.factory;

import com.g4vrk.react.runner.TaskRunner;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractTaskRunnerFactory<R extends TaskRunner> implements TaskRunnerFactory {
    @Getter(AccessLevel.PROTECTED)
    private final Plugin plugin;

    protected AbstractTaskRunnerFactory(
            @NotNull Plugin plugin
    ) {
        this.plugin = plugin;
    }

    @Override
    public abstract @NotNull R create();
}
