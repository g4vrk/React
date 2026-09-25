package com.g4vrk.react.listeners.bukkit;

import com.g4vrk.react.React;
import com.g4vrk.react.api.channel.ReactChannels;
import com.g4vrk.react.player.factory.PlayerFactory;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import com.g4vrk.react.storage.StorageManager;
import com.g4vrk.react.storage.model.PlayerStorageData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Consumer;

public class ConnectionListener implements Listener {

    private final Logger logger = React.INSTANCE.getLogger();

    private final PlayerRegistry playerRegistry;
    private final PlayerFactory playerFactory;
    private final StorageManager storageManager;

    private final Consumer<Player> onJoin;
    private final Consumer<Player> onQuit;

    public ConnectionListener(
            @NotNull PlayerRegistry playerRegistry,
            @NotNull PlayerFactory playerFactory,
            @NotNull StorageManager storageManager,
            @NotNull Consumer<Player> onJoin,
            @NotNull Consumer<Player> onQuit
    ) {
        this.playerRegistry = playerRegistry;
        this.playerFactory = playerFactory;
        this.storageManager = storageManager;
        this.onJoin = onJoin;
        this.onQuit = onQuit;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUserLogin(@NotNull PlayerJoinEvent event) {
        try {
            registerPlayer(event.getPlayer());
        } catch (final Exception ex) {
            logger.error("Could not handle login of a player", ex);
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUserDisconnect(@NotNull PlayerQuitEvent event) {
        try {

            final Player bukkitPlayer = event.getPlayer();
            final UUID uniqueId = bukkitPlayer.getUniqueId();

            final ReactPlayer player = playerRegistry.getPlayer(uniqueId);
            if (player != null) {
                storageManager.saveSnapshot(player);
            }

            playerRegistry.removePlayer(uniqueId);

            ReactChannels.ALERTS.remove(bukkitPlayer);
            ReactChannels.VERBOSE.remove(bukkitPlayer);

            onQuit.accept(bukkitPlayer);

        } catch (final Exception ex) {
            logger.error("Could not handle disconnect of a player", ex);
        }
    }

    public void registerPlayer(final @NotNull Player bukkitPlayer) {
        final UUID uniqueId = bukkitPlayer.getUniqueId();
        final ReactPlayer entity = playerFactory.create(uniqueId, bukkitPlayer.getName(), bukkitPlayer);

        if (entity != null) {
            playerRegistry.addPlayer(uniqueId, entity);
            storageManager.loadPlayer(uniqueId).whenComplete((data, failure) -> {
                try {
                    if (playerRegistry.getPlayer(uniqueId) != entity) {
                        return;
                    }
                    if (failure == null) {
                        entity.applyStorageData(data == null ? PlayerStorageData.EMPTY : data);
                    } else {
                        logger.debug("Could not load stored data for {}; checks will continue in fail-open mode", entity.getName(), failure);
                        retryHydration(entity);
                    }
                } finally {
                    entity.markDataReady();
                }
            });

            onJoin.accept(bukkitPlayer);
        }
    }

    private void retryHydration(final @NotNull ReactPlayer entity) {
        storageManager.loadPlayerWhenAvailable(entity.getUniqueId())
                .whenComplete((data, failure) -> {
                    if (playerRegistry.getPlayer(entity.getUniqueId()) != entity) {
                        return;
                    }
                    if (failure != null) {
                        return;
                    }
                    entity.mergeStorageData(data == null ? PlayerStorageData.EMPTY : data);
                    storageManager.saveSnapshot(entity);
                });
    }
}
