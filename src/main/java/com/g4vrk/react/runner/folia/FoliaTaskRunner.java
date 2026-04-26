package com.g4vrk.react.runner.folia;

import com.g4vrk.react.runner.AbstractTaskRunner;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class FoliaTaskRunner extends AbstractTaskRunner {

    private final GlobalRegionScheduler globalRegionScheduler;
    private final RegionScheduler regionScheduler;
    private final AsyncScheduler asyncScheduler;

    public FoliaTaskRunner(@NotNull Plugin plugin) {
        super(plugin);
        this.regionScheduler = Bukkit.getRegionScheduler();
        this.globalRegionScheduler = Bukkit.getGlobalRegionScheduler();
        this.asyncScheduler = Bukkit.getAsyncScheduler();
    }

    @Override
    public void runTask(@NotNull Runnable runnable) {
        globalRegionScheduler.run(getPlugin(), scheduledTask -> runnable.run());
    }

    @Override
    public void runTaskAsynchronously(@NotNull Runnable runnable) {
        asyncScheduler.runNow(getPlugin(), scheduledTask -> runnable.run());
    }

    @Override
    public void runTaskLaterAsynchronously(long durationTicks, @NotNull Runnable runnable) {
        asyncScheduler.runDelayed(getPlugin(), scheduledTask -> runnable.run(), durationTicks * 50L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runGlobally(@NotNull Runnable runnable) {
        runTask(runnable);
    }

    @Override
    public void runEntity(@NotNull Entity entity, @NotNull Runnable runnable) {
        entity.getScheduler().run(getPlugin(), scheduledTask -> runnable.run(), null);
    }

    @Override
    public void runRegion(@NotNull Location location, @NotNull Runnable runnable) {
        regionScheduler.run(
                getPlugin(),
                location,
                task -> runnable.run()
        );
    }
}
