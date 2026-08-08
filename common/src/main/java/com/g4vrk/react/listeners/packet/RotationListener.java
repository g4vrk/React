package com.g4vrk.react.listeners.packet;

import com.g4vrk.react.check.processor.rotation.RotationProcessor;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class RotationListener extends PacketListenerAbstract {

    private final PlayerRegistry playerRegistry;
    private final RotationProcessor rotationProcessor;

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {

        if (event.getConnectionState() != ConnectionState.PLAY || !WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        final WrapperPlayClientPlayerFlying movePacket;

        try {
            movePacket = new WrapperPlayClientPlayerFlying(event);
        } catch (Throwable ignored) {
            return;
        }

        if (!movePacket.hasRotationChanged()) return;

        final ReactPlayer player = this.playerRegistry.getPlayer(event.getUser().getUUID());

        if (player == null) return;

        final Location location = movePacket.getLocation();

        this.rotationProcessor.process(
                player,
                location.getYaw(),
                location.getPitch()
        );

    }

}