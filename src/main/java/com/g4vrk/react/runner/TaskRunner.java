package com.g4vrk.react.runner;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public interface TaskRunner {
    void runTask(@NotNull Runnable runnable);
    void runTaskAsynchronously(@NotNull Runnable runnable);
    void runTaskLaterAsynchronously(long durationTicks, @NotNull Runnable runnable);
    void runGlobally(@NotNull Runnable runnable);
    void runEntity(@NotNull Entity entity, @NotNull Runnable runnable);
    void runRegion(@NotNull Location location, @NotNull Runnable runnable);
}
