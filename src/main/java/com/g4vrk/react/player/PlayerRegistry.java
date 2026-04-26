package com.g4vrk.react.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerRegistry {

    private static final Map<UUID, LocalPlayer> PLAYERS = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerActivity> ACTIVITY = new ConcurrentHashMap<>();

    public static void addPlayer(UUID uuid, LocalPlayer entity) {
        PLAYERS.put(uuid, entity);
        ACTIVITY.put(uuid, new PlayerActivity());
    }

    public static void removePlayer(UUID uuid) {
        PLAYERS.remove(uuid);
        ACTIVITY.remove(uuid);
    }

    public static LocalPlayer getPlayer(UUID uuid) {
        LocalPlayer entity = PLAYERS.get(uuid);

        if (entity == null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return null;

            entity = new LocalPlayer(
                    player.getUniqueId(),
                    player.getName(),
                    150
            );

            PLAYERS.put(uuid, entity);
            ACTIVITY.put(uuid, new PlayerActivity());
        }

        return entity;
    }

    public static PlayerActivity getActivity(UUID uuid) {
        return ACTIVITY.get(uuid);
    }
}