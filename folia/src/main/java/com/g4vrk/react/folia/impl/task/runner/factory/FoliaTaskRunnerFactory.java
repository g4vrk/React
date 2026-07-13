package com.g4vrk.react.folia.impl.task.runner.factory;

import com.g4vrk.react.api.task.runner.factory.AbstractTaskRunnerFactory;
import com.g4vrk.react.folia.impl.task.runner.FoliaTaskRunner;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class FoliaTaskRunnerFactory extends AbstractTaskRunnerFactory<FoliaTaskRunner> {
    public FoliaTaskRunnerFactory(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    public @NonNull FoliaTaskRunner create() {
        return new FoliaTaskRunner(getPlugin());
    }
}
