package com.g4vrk.react.alert.publish.impl;

import com.g4vrk.react.alert.publish.Publisher;
import com.g4vrk.react.api.task.runner.TaskRunner;
import com.g4vrk.react.api.task.schedule.TickSchedule;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public class AlertPublisher implements Publisher<Component> {

    private final Server server;
    private final Executor executor;
    private final TaskRunner taskRunner;
    private final Predicate<Audience> filter;
    private final boolean includeConsole;
    private final Set<Audience> listeners = ConcurrentHashMap.newKeySet();

    public AlertPublisher(
            @NotNull Server server,
            @NotNull Executor executor,
            @NotNull TaskRunner taskRunner,
            @NotNull Predicate<Audience> filter,
            boolean includeConsole
    ) {
        this.server = server;
        this.executor = executor;
        this.taskRunner = taskRunner;
        this.filter = filter;
        this.includeConsole = includeConsole;
    }

    @Override
    public void publish(@NotNull Component input) {
        broadcast(input);
    }

    private void broadcast(
            final @NotNull Component input
    ) {
        taskRunner.runTask(() -> {
            for (final Audience listener : listeners) {
                try {
                    listener.sendMessage(input);
                } catch (Throwable ignored) {}
            }
        }, TickSchedule.instant());
    }

    @SuppressWarnings("UnusedReturnValue")
    public @NotNull CompletableFuture<Void> flushAsync() {
        return CompletableFuture.runAsync(this::flushListeners, executor);
    }

    public void flushListeners() {
        listeners.clear();

        for (final Audience audience : server.getOnlinePlayers()) {
            if (filter.test(audience)) {
                listeners.add(audience);
            }
        }

        if (includeConsole) {
            listeners.add(server.getConsoleSender());
        }
    }

}