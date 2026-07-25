package com.g4vrk.react.alert.manager;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {

    private final Set<UUID> receivers = ConcurrentHashMap.newKeySet();

    public AlertManager() {
    }

    public boolean add(
            final @NotNull UUID uuid
    ) {
        return this.receivers.add(uuid);
    }

    public boolean remove(
            final @NotNull UUID uuid
    ) {
        return this.receivers.remove(uuid);
    }

    public boolean receives(
            final @NotNull UUID uuid
    ) {
        return this.receivers.contains(uuid);
    }

}
