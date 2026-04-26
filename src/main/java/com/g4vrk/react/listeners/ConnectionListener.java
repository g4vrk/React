package com.g4vrk.react.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import com.g4vrk.react.player.LocalPlayer;
import com.g4vrk.react.player.PlayerRegistry;

public class ConnectionListener extends PacketListenerAbstract {

    private final int bufferSize;

    public ConnectionListener(int bufferSize) {
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

        PlayerRegistry.addPlayer(user.getUUID(), entity);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        final User user = event.getUser();

        PlayerRegistry.removePlayer(user.getUUID());
    }
}