package com.g4vrk.react.listeners.packet;

import com.g4vrk.react.React;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import com.g4vrk.react.universe.UniverseService;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import java.util.UUID;
import org.bukkit.entity.Player;

/** SHARD receives attacks and rotations at Raveon's LOW priority. */
public final class UniverseListener extends PacketListenerAbstract {
    private final PlayerRegistry players;
    private final UniverseService service;

    public UniverseListener(PlayerRegistry players, UniverseService service) {
        super(PacketListenerPriority.LOW);
        this.players = players;
        this.service = service;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY || event.getUser() == null
                || event.getUser().getUUID() == null) {
            return;
        }
        UUID uuid = event.getUser().getUUID();
        ReactPlayer player = players.getPlayer(uuid);
        try {
            if (player != null) {
                service.onPacket(player, event);
            } else {
                Object rawPlayer = event.getPlayer();
                service.onPacketBeforeJoin(uuid, rawPlayer instanceof Player bukkitPlayer ? bukkitPlayer : null, event);
            }
        } catch (RuntimeException exception) {
            React.INSTANCE.getLogger().debug("Could not process UNIVERSE packet", exception);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        if (event.getUser() != null && event.getUser().getUUID() != null) {
            service.removePlayerData(event.getUser().getUUID());
        }
    }
}
