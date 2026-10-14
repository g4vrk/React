package com.g4vrk.reactdataset.listeners.packet;

import com.g4vrk.react.player.registry.PlayerRegistry;
import com.g4vrk.reactdataset.collector.DatasetCollector;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class RotationListener
extends PacketListenerAbstract {
    private final PlayerRegistry playerRegistry;
    private final DatasetCollector collector;

    public RotationListener(PlayerRegistry playerRegistry, DatasetCollector collector) {
        this.playerRegistry = playerRegistry;
        this.collector = collector;
    }

    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        WrapperPlayClientPlayerFlying packet;
        if (event.getConnectionState() != ConnectionState.PLAY || !WrapperPlayClientPlayerFlying.isFlying((PacketTypeCommon)event.getPacketType())) {
            return;
        }
        try {
            packet = new WrapperPlayClientPlayerFlying(event);
        }
        catch (Throwable ignored) {
            return;
        }
        if (!packet.hasRotationChanged()) {
            return;
        }
        UUID uuid = event.getUser().getUUID();
        if (this.playerRegistry.getPlayer(uuid) == null) {
            return;
        }
        Location location = packet.getLocation();
        this.collector.onRotation(uuid, location.getYaw(), location.getPitch());
    }
}
