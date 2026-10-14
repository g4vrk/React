package com.g4vrk.react.universe;

import com.g4vrk.schedula.task.TickSchedule;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/** Uses the same entity tick phase as Raveon, including modern Paper. */
public final class UniverseEntityRunner {
    private final Plugin plugin;
    private final Scheduler fallback;

    public UniverseEntityRunner(Plugin plugin, Scheduler fallback) {
        this.plugin = plugin;
        this.fallback = fallback;
    }

    public void run(Entity entity, Runnable task) {
        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            for (Method method : scheduler.getClass().getMethods()) {
                if (method.getName().equals("run") && method.getParameterCount() == 3) {
                    method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> task.run(), (Runnable) () -> {});
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            // Older Paper has no EntityScheduler; retain the existing platform fallback.
        }
        fallback.scheduleEntity(entity, task, TickSchedule.instant());
    }
}
