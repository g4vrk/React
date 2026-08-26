package com.g4vrk.react.player.registry;

import com.g4vrk.react.player.ReactPlayer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerRegistry {

    private final Map<UUID, ReactPlayer> players = new ConcurrentHashMap<>();

    public void addPlayer(
            final @NotNull UUID uuid,
            final @NotNull ReactPlayer entity
    ) {
        players.put(uuid, entity);
    }

    public void removePlayer(
            final @NotNull UUID uuid
    ) {
        players.remove(uuid);
    }

    public @Nullable ReactPlayer getPlayer(
            final @NotNull UUID uuid
    ) {

        return players.get(uuid);

    }

    public void clear() {
        players.clear();
    }

    public @NotNull Set<ReactPlayer> all() {
        return new ObjectOpenHashSet<>(players.values());
    }
}
