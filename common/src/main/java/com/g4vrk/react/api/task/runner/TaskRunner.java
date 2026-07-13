package com.g4vrk.react.api.task.runner;

import com.g4vrk.react.api.task.Task;
import com.g4vrk.react.api.task.schedule.TickSchedule;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public interface TaskRunner {

    @NotNull Task runTask(@NotNull Runnable runnable, @NotNull TickSchedule tickSchedule);

    @NotNull Task runTaskAsynchronously(@NotNull Runnable runnable, @NotNull TickSchedule tickSchedule);

    @NotNull Task runGlobally(@NotNull Runnable runnable, @NotNull TickSchedule tickSchedule);

    @NotNull Task runEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull TickSchedule tickSchedule);

    @NotNull Task runRegion(@NotNull Location location, @NotNull Runnable runnable, @NotNull TickSchedule tickSchedule);

}
