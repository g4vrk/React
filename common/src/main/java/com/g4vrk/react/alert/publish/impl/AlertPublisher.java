package com.g4vrk.react.alert.publish.impl;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.alert.publish.Publisher;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.schedula.task.TickSchedule;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public class AlertPublisher implements Publisher<Component>, ReloadObserver {

    private final Server server;
    private final Executor executor;
    private final Scheduler scheduler;
    private final Predicate<Audience> filter;
    private final Set<Audience> listeners = ConcurrentHashMap.newKeySet();

    private boolean includeConsole;

    public AlertPublisher(
            @NotNull Server server,
            @NotNull Executor executor,
            @NotNull Scheduler scheduler,
            @NotNull Predicate<Audience> filter
    ) {
        this.server = server;
        this.executor = executor;
        this.scheduler = scheduler;
        this.filter = filter;

        this.reload();
    }

    public void reload() {

        final Config config = React.INSTANCE.getMainConfig();

        this.onReload(config);

    }

    @Override
    public void onReload(@NotNull Config config) {

        this.includeConsole = config.node("alerts", "show-in-console")
                .getBoolean(true);

    }

    @Override
    public void publish(@NotNull Component input) {
        broadcast(input);
    }

    private void broadcast(
            final @NotNull Component input
    ) {
        scheduler.schedule(() -> {
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