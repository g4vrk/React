package com.g4vrk.react.player.registry;

import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.React;
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
        final ReactPlayer previous = players.put(uuid, entity);
        if (previous != null) {
            previous.clearTransientState();
        }
    }

    public void removePlayer(
            final @NotNull UUID uuid
    ) {
        final ReactPlayer removed = players.remove(uuid);
        if (removed != null) {
            removed.clearTransientState();
            if (React.INSTANCE.getUniverseService() != null) {
                React.INSTANCE.getUniverseService().removePlayerData(uuid);
            }
        }
    }

    public @Nullable ReactPlayer getPlayer(
            final @NotNull UUID uuid
    ) {

        return players.get(uuid);

    }

    public @Nullable ReactPlayer getByEntityId(final int entityId) {
        for (final ReactPlayer player : players.values()) {
            if (player.bukkitPlayer.getEntityId() == entityId) {
                return player;
            }
        }
        return null;
    }

    public void clear() {
        if (React.INSTANCE.getUniverseService() != null) {
            players.keySet().forEach(React.INSTANCE.getUniverseService()::removePlayerData);
        }
        players.values().forEach(ReactPlayer::clearTransientState);
        players.clear();
    }

    public @NotNull Set<ReactPlayer> all() {
        return new ObjectOpenHashSet<>(players.values());
    }
}
