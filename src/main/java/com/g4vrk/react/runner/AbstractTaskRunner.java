package com.g4vrk.react.runner;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractTaskRunner implements TaskRunner {
    @Getter(AccessLevel.PROTECTED)
    private final Plugin plugin;

    protected AbstractTaskRunner(
            @NotNull Plugin plugin
    ) {
        this.plugin = plugin;
    }

    @Override
    public abstract void runTask(@NotNull Runnable runnable);

    @Override
    public abstract void runTaskAsynchronously(@NotNull Runnable runnable);

    @Override
    public abstract void runTaskLaterAsynchronously(long durationTicks, @NotNull Runnable runnable);

    @Override
    public abstract void runGlobally(@NotNull Runnable runnable);

    @Override
    public abstract void runEntity(@NotNull Entity entity, @NotNull Runnable runnable);

    @Override
    public abstract void runRegion(@NotNull Location location, @NotNull Runnable runnable);
}
