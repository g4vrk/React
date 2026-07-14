package com.g4vrk.react.check.type;

import com.g4vrk.react.check.ReactCheck;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.jetbrains.annotations.NotNull;

public interface PacketCheck extends ReactCheck {

    default void onPacketReceive(@NotNull PacketReceiveEvent event) {
    }

    default void onPacketSend(@NotNull PacketSendEvent event) {
    }

}
