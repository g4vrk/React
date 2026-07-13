package com.g4vrk.react.paper.impl.task.runner.factory;

import com.g4vrk.react.api.task.runner.factory.AbstractTaskRunnerFactory;
import com.g4vrk.react.paper.impl.task.runner.PaperTaskRunner;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PaperTaskRunnerFactory extends AbstractTaskRunnerFactory<PaperTaskRunner> {
    public PaperTaskRunnerFactory(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    public @NotNull PaperTaskRunner create() {
        return new PaperTaskRunner(getPlugin());
    }
}
