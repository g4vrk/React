package com.g4vrk.react.player.factory;

import com.g4vrk.react.player.model.ReactPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
public final class PlayerFactory {

    private final int rotationsBufferSize;

    public @NotNull ReactPlayer create(
            final @NotNull UUID uniqueId,
            final @NotNull String name,
            final @NotNull Player bukkitPlayer
    ) {
        return new ReactPlayer(uniqueId, name, bukkitPlayer, rotationsBufferSize);
    }

}
