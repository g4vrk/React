package com.g4vrk.react.player.registry;

import com.g4vrk.react.player.CombatActivity;
import com.g4vrk.react.player.factory.PlayerFactory;
import com.g4vrk.react.player.model.ReactPlayer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
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

    private final Map<UUID, ReactPlayer> players = new Object2ObjectOpenHashMap<>();

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

            final Player player = this.findBukkitPlayer(uuid);

            if (player == null) return null;

            return factory.create(uniqueId, player.getName());

        });

    }

    private @Nullable Player findBukkitPlayer(
            final @NotNull UUID uuid
    ) {
        return getServer().getPlayer(uuid);
    }
}