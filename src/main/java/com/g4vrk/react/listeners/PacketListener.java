package com.g4vrk.react.listeners;

import com.g4vrk.react.game.Rotation;
import com.g4vrk.react.ml.check.MLCheck;
import com.g4vrk.react.player.PlayerActivity;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.g4vrk.react.game.AngleMath;
import com.g4vrk.react.player.LocalPlayer;
import com.g4vrk.react.player.PlayerRegistry;
import com.g4vrk.react.player.RotationState;
import org.jetbrains.annotations.NotNull;

public final class PacketListener extends PacketListenerAbstract {

    private final MLCheck analyzer;

    public PacketListener(
            final @NotNull MLCheck analyzer
    ) {
        this.analyzer = analyzer;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        final boolean movePacket = WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());

        if (event.getConnectionState() != ConnectionState.PLAY || !movePacket) return;

        final WrapperPlayClientPlayerFlying flying;
        try {
            flying = new WrapperPlayClientPlayerFlying(event);
        } catch (final Throwable th) {
            return;
        }

        if (!flying.hasRotationChanged()) return;

        final User user = event.getUser();

        final LocalPlayer entity = PlayerRegistry.getPlayer(user.getUUID());
        final PlayerActivity activity = PlayerRegistry.getActivity(user.getUUID());
        if (entity == null || activity == null) return;

        final RotationState rotationState = entity.getMovement();

        float yaw = AngleMath.normalizeAngle(flying.getLocation().getYaw());
        float pitch = AngleMath.normalizeAngle(flying.getLocation().getPitch());

        float deltaYaw = yaw - rotationState.getYaw();
        float deltaPitch = pitch - rotationState.getPitch();

        float accelYaw = AngleMath.calculateAcceleration(deltaYaw, rotationState.getDeltaYaw());
        float accelPitch = AngleMath.calculateAcceleration(deltaPitch, rotationState.getDeltaPitch());

        final Rotation rotation = new Rotation(
                deltaYaw,
                deltaPitch,
                accelYaw,
                accelPitch,
                AngleMath.calculateJerk(accelYaw, rotationState.getAccelYaw()),
                AngleMath.calculateJerk(accelPitch, rotationState.getAccelPitch()),
                AngleMath.calculateGCDError(deltaYaw),
                AngleMath.calculateGCDError(deltaPitch)
        );

        rotationState.setYaw(yaw);
        rotationState.setPitch(pitch);
        rotationState.setDeltaYaw(deltaYaw);
        rotationState.setDeltaPitch(deltaPitch);
        rotationState.setAccelYaw(accelYaw);
        rotationState.setAccelPitch(accelPitch);

        entity.addRotation(rotation);

        analyzer.onRotation(entity, activity, rotation);
    }
}