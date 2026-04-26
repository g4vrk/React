package com.g4vrk.react.runner.paper;

import com.g4vrk.react.runner.AbstractTaskRunner;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public class PaperTaskRunner extends AbstractTaskRunner {

    private final BukkitScheduler scheduler;

    public PaperTaskRunner(
            @NotNull Plugin plugin
    ) {
        super(plugin);
        this.scheduler = plugin.getServer().getScheduler();
    }

    @Override
    public void runTask(
            @NotNull Runnable runnable
    ) {
        this.scheduler.runTask(
                super.getPlugin(),
                runnable
        );
    }

    @Override
    public void runTaskAsynchronously(
            @NotNull Runnable runnable
    ) {
        this.scheduler.runTaskAsynchronously(
                super.getPlugin(),
                runnable
        );
    }

    @Override
    public void runGlobally(@NotNull Runnable runnable) {
        runTask(runnable);
    }

    @Override
    public void runEntity(@NotNull Entity entity, @NotNull Runnable runnable) {
        runTask(runnable);
    }

    @Override
    public void runRegion(@NotNull Location location, @NotNull Runnable runnable) {
        runTask(runnable);
    }
}
