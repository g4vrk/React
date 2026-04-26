package com.g4vrk.react.runner.paper.factory;

import com.g4vrk.react.runner.factory.AbstractTaskRunnerFactory;
import com.g4vrk.react.runner.paper.PaperTaskRunner;
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
