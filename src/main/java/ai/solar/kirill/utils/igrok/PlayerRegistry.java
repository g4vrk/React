package ai.solar.kirill.utils.igrok;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegistry {
    private static final Map<UUID, PlayerEntity> players = new ConcurrentHashMap<>();

    public static void addPlayer(UUID uuid, PlayerEntity entity) {
        players.put(uuid, entity);
    }

    public static void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public static PlayerEntity getPlayer(UUID uuid) {
        PlayerEntity entity = players.get(uuid);
        if (entity == null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                entity = new PlayerEntity(player.getUniqueId(), player.getName());
                PlayerEntity existing = players.putIfAbsent(uuid, entity);
                if (existing != null) {
                    entity = existing;
                }
            }
        }
        return entity;
    }
}