package com.g4vrk.react.player;

import com.g4vrk.react.player.model.ReactPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerRegistry {

    private final int bufferSize;

    private final Map<UUID, ReactPlayer> players = new ConcurrentHashMap<>();

    public PlayerRegistry(
            int bufferSize
    ) {
        this.bufferSize = bufferSize;
    }

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
        return players.computeIfAbsent(uuid, uniqueId -> {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) return null;

            return new ReactPlayer(
                    player.getUniqueId(),
                    player.getName(),
                    bufferSize
            );
        });
    }

    public @Nullable CombatActivity getActivity(
            final @NotNull UUID uuid
    ) {
        return players.get(uuid).getCombatActivity();
    }
}