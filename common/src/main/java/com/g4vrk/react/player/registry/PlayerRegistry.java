package com.g4vrk.react.player.registry;

import com.g4vrk.react.player.factory.PlayerFactory;
import com.g4vrk.react.player.model.ReactPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.bukkit.Bukkit.getServer;

@RequiredArgsConstructor
public final class PlayerRegistry {

    private final PlayerFactory factory;

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

        return players.computeIfAbsent(uuid, uniqueId -> {

            final Player bukkitPlayer = this.findBukkitPlayer(uniqueId);

            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return null;

            return factory.create(uniqueId, bukkitPlayer.getName(), bukkitPlayer);

        });

    }

    public void clear() {
        players.clear();
    }

    private @Nullable Player findBukkitPlayer(
            final @NotNull UUID uuid
    ) {
        return getServer().getPlayer(uuid);
    }
}