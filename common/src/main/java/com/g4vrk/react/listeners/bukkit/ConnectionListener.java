package com.g4vrk.react.listeners.bukkit;

import com.g4vrk.react.Permissions;
import com.g4vrk.react.React;
import com.g4vrk.react.alert.manager.AlertManager;
import com.g4vrk.react.alert.publish.impl.AlertPublisher;
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

public class ConnectionListener implements Listener {

    private final Logger logger = React.INSTANCE.getLogger();

    private final PlayerRegistry playerRegistry;
    private final PlayerFactory playerFactory;
    private final AlertPublisher alertPublisher;
    private final AlertManager alertManager;
    private final StorageManager storageManager;

    public ConnectionListener(
            @NotNull PlayerRegistry playerRegistry,
            @NotNull PlayerFactory playerFactory,
            @NotNull AlertPublisher alertPublisher,
            @NotNull AlertManager alertManager,
            @NotNull StorageManager storageManager
    ) {
        this.playerRegistry = playerRegistry;
        this.playerFactory = playerFactory;
        this.alertPublisher = alertPublisher;
        this.alertManager = alertManager;
        this.storageManager = storageManager;
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
            final UUID uniqueId = event.getPlayer().getUniqueId();

            final ReactPlayer player = playerRegistry.getPlayer(uniqueId);
            if (player != null) {
                storageManager.saveSnapshot(player);
            }

            alertManager.remove(uniqueId);
            playerRegistry.removePlayer(uniqueId);

            alertPublisher.flushAsync();
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
        }

        if (bukkitPlayer.hasPermission(Permissions.ALERTS_ENABLE_ON_JOIN)) {
            alertManager.add(uniqueId);
        }
        alertPublisher.flushAsync();
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
