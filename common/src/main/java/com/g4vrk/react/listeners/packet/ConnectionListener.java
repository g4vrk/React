package com.g4vrk.react.listeners.packet;

import com.g4vrk.react.alert.publish.impl.AlertPublisher;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import com.g4vrk.react.player.model.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import org.jetbrains.annotations.NotNull;

public class ConnectionListener extends PacketListenerAbstract {

    private final PlayerRegistry playerRegistry;
    private final AlertPublisher alertPublisher;
    private final int bufferSize;

    public ConnectionListener(
            @NotNull PlayerRegistry playerRegistry,
            @NotNull AlertPublisher alertPublisher,
            int bufferSize
    ) {
        this.playerRegistry = playerRegistry;
        this.alertPublisher = alertPublisher;
        this.bufferSize = bufferSize;
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        final User user = event.getUser();

        final ReactPlayer entity = new ReactPlayer(
                user.getUUID(),
                user.getName(),
                bufferSize
        );

        playerRegistry.addPlayer(user.getUUID(), entity);

        alertPublisher.flushAsync();
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        final User user = event.getUser();

        playerRegistry.removePlayer(user.getUUID());

        alertPublisher.flushAsync();
    }
}