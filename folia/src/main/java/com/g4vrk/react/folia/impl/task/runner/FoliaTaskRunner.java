package com.g4vrk.react.folia.impl.task.runner;

import com.g4vrk.react.api.task.DummyTask;
import com.g4vrk.react.api.task.Task;
import com.g4vrk.react.api.task.runner.AbstractTaskRunner;
import com.g4vrk.react.api.task.schedule.TickSchedule;
import com.g4vrk.react.folia.impl.task.FoliaTask;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class FoliaTaskRunner extends AbstractTaskRunner {

    private final GlobalRegionScheduler globalRegionScheduler;

    private final RegionScheduler regionScheduler;

    private final AsyncScheduler asyncScheduler;

    public FoliaTaskRunner(
            @NotNull Plugin plugin
    ) {
        super(plugin);

        this.regionScheduler = super.getServer().getRegionScheduler();
        this.globalRegionScheduler = super.getServer().getGlobalRegionScheduler();
        this.asyncScheduler = super.getServer().getAsyncScheduler();
    }

    @Override
    public @NotNull Task runTask(
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {
        return runGlobally(
                runnable,
                tickSchedule
        );
    }

    @Override
    public @NotNull Task runTaskAsynchronously(
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {

        final ScheduledTask task;

        if (tickSchedule.isRepeating()) {

            task = asyncScheduler.runAtFixedRate(
                    getPlugin(),
                    scheduledTask -> runnable.run(),
                    toMillis(tickSchedule.getDelay()),
                    toMillis(tickSchedule.getPeriod()),
                    TimeUnit.MILLISECONDS
            );

        } else if (tickSchedule.isDelayed()) {

            task = asyncScheduler.runDelayed(
                    getPlugin(),
                    scheduledTask -> runnable.run(),
                    toMillis(tickSchedule.getDelay()),
                    TimeUnit.MILLISECONDS
            );

        } else {

            task = asyncScheduler.runNow(
                    getPlugin(),
                    scheduledTask -> runnable.run()
            );
        }

        return adapt(task);
    }

    @Override
    public @NotNull Task runGlobally(
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {

        final ScheduledTask task;

        if (tickSchedule.isRepeating()) {

            task = globalRegionScheduler.runAtFixedRate(
                    getPlugin(),
                    scheduledTask -> runnable.run(),
                    tickSchedule.getDelay(),
                    tickSchedule.getPeriod()
            );

        } else if (tickSchedule.isDelayed()) {

            task = globalRegionScheduler.runDelayed(
                    getPlugin(),
                    scheduledTask -> runnable.run(),
                    tickSchedule.getDelay()
            );

        } else {

            task = globalRegionScheduler.run(
                    getPlugin(),
                    scheduledTask -> runnable.run()
            );
        }

        return adapt(task);
    }

    @Override
    public @NotNull Task runEntity(
            @NotNull Entity entity,
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {

        final ScheduledTask task;

        if (tickSchedule.isRepeating()) {

            task = entity.getScheduler().runAtFixedRate(
                    getPlugin(),
                    scheduledTask -> runnable.run(),
                    null,
                    tickSchedule.getDelay(),
                    tickSchedule.getPeriod()
            );

        } else if (tickSchedule.isDelayed()) {

            task = entity.getScheduler().runDelayed(
                    getPlugin(),
                    scheduledTask -> runnable.run(),
                    null,
                    tickSchedule.getDelay()
            );

        } else {

            task = entity.getScheduler().run(
                    getPlugin(),
                    scheduledTask -> runnable.run(),
                    null
            );
        }

        if (task == null) {
            return DummyTask.get();
        }

        return adapt(task);
    }

    @Override
    public @NotNull Task runRegion(
            @NotNull Location location,
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {

        final ScheduledTask task;

        if (tickSchedule.isRepeating()) {

            task = regionScheduler.runAtFixedRate(
                    getPlugin(),
                    location,
                    scheduledTask -> runnable.run(),
                    tickSchedule.getDelay(),
                    tickSchedule.getPeriod()
            );

        } else if (tickSchedule.isDelayed()) {

            task = regionScheduler.runDelayed(
                    getPlugin(),
                    location,
                    scheduledTask -> runnable.run(),
                    tickSchedule.getDelay()
            );

        } else {

            task = regionScheduler.run(
                    getPlugin(),
                    location,
                    scheduledTask -> runnable.run()
            );
        }

        return adapt(task);
    }

    private long toMillis(
            final long ticks
    ) {
        return ticks * 50L;
    }

    private @NotNull Task adapt(
            @NotNull ScheduledTask task
    ) {
        return new FoliaTask(task);
    }
}