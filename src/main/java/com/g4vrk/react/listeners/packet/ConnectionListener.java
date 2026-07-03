package com.g4vrk.react.listeners.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import com.g4vrk.react.player.model.LocalPlayer;
import com.g4vrk.react.player.PlayerRegistry;
import org.jetbrains.annotations.NotNull;

public class ConnectionListener extends PacketListenerAbstract {

    private final PlayerRegistry playerRegistry;
    private final int bufferSize;

    public ConnectionListener(
            @NotNull PlayerRegistry playerRegistry,
            int bufferSize
    ) {
        this.playerRegistry = playerRegistry;
        this.bufferSize = bufferSize;
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        final User user = event.getUser();

        final LocalPlayer entity = new LocalPlayer(
                user.getUUID(),
                user.getName(),
                bufferSize
        );

        playerRegistry.addPlayer(user.getUUID(), entity);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        final User user = event.getUser();

        playerRegistry.removePlayer(user.getUUID());
    }
}