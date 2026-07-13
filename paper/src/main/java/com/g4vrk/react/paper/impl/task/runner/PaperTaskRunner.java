package com.g4vrk.react.paper.impl.task.runner;

import com.g4vrk.react.api.task.Task;
import com.g4vrk.react.api.task.runner.AbstractTaskRunner;
import com.g4vrk.react.api.task.schedule.TickSchedule;
import com.g4vrk.react.paper.impl.task.PaperTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public class PaperTaskRunner extends AbstractTaskRunner {

    private final BukkitScheduler scheduler;

    public PaperTaskRunner(@NotNull Plugin plugin) {
        super(plugin);
        this.scheduler = plugin.getServer().getScheduler();
    }

    private @NotNull Task schedule(
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule,
            boolean async
    ) {

        final long delay = tickSchedule.getDelay();
        final long period = tickSchedule.getPeriod();

        if (tickSchedule.isRepeating()) {

            return new PaperTask(
                    async
                            ? scheduler.runTaskTimerAsynchronously(
                            getPlugin(),
                            runnable,
                            delay,
                            period
                    )
                            : scheduler.runTaskTimer(
                            getPlugin(),
                            runnable,
                            delay,
                            period
                    )
            );
        }

        if (tickSchedule.isDelayed()) {

            return new PaperTask(
                    async
                            ? scheduler.runTaskLaterAsynchronously(
                            getPlugin(),
                            runnable,
                            delay
                    )
                            : scheduler.runTaskLater(
                            getPlugin(),
                            runnable,
                            delay
                    )
            );
        }

        return new PaperTask(
                async
                        ? scheduler.runTaskAsynchronously(
                        getPlugin(),
                        runnable
                )
                        : scheduler.runTask(
                        getPlugin(),
                        runnable
                )
        );
    }

    @Override
    public @NotNull Task runTask(
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {
        return schedule(
                runnable,
                tickSchedule,
                false
        );
    }

    @Override
    public @NotNull Task runTaskAsynchronously(
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {
        return schedule(
                runnable,
                tickSchedule,
                true
        );
    }

    @Override
    public @NotNull Task runGlobally(
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {
        return runTask(
                runnable,
                tickSchedule
        );
    }

    @Override
    public @NotNull Task runEntity(
            @NotNull Entity entity,
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {
        return runTask(
                runnable,
                tickSchedule
        );
    }

    @Override
    public @NotNull Task runRegion(
            @NotNull Location location,
            @NotNull Runnable runnable,
            @NotNull TickSchedule tickSchedule
    ) {
        return runTask(
                runnable,
                tickSchedule
        );
    }
}
