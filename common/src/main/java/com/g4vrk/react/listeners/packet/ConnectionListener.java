package com.g4vrk.react.listeners.packet;

import com.g4vrk.react.Permissions;
import com.g4vrk.react.alert.manager.AlertManager;
import com.g4vrk.react.alert.publish.impl.AlertPublisher;
import com.g4vrk.react.player.factory.PlayerFactory;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import com.g4vrk.react.player.model.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConnectionListener extends PacketListenerAbstract {

    private final PlayerRegistry playerRegistry;
    private final PlayerFactory playerFactory;
    private final AlertPublisher alertPublisher;
    private final AlertManager alertManager;

    public ConnectionListener(
            @NotNull PlayerRegistry playerRegistry,
            @NotNull PlayerFactory playerFactory,
            @NotNull AlertPublisher alertPublisher,
            @NotNull AlertManager alertManager
    ) {
        this.playerRegistry = playerRegistry;
        this.playerFactory = playerFactory;
        this.alertPublisher = alertPublisher;
        this.alertManager = alertManager;
    }

    @Override
    public void onUserLogin(@NotNull UserLoginEvent event) {
        try {
            final User user = event.getUser();
            final Player bukkitPlayer = event.getPlayer();

            if (bukkitPlayer == null) return;

            final ReactPlayer entity = playerFactory.create(user.getUUID(), user.getName(), bukkitPlayer);

            playerRegistry.addPlayer(user.getUUID(), entity);

            if (bukkitPlayer.hasPermission(Permissions.ALERTS_ENABLE_ON_JOIN)) {
                alertManager.add(bukkitPlayer.getUniqueId());
            }

            alertPublisher.flushAsync();
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public void onUserDisconnect(@NotNull UserDisconnectEvent event) {
        try {
            final User user = event.getUser();

            playerRegistry.removePlayer(user.getUUID());

            alertPublisher.flushAsync();
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}