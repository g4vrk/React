package com.g4vrk.react.player.factory;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.player.model.ReactPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlayerFactory implements ReloadObserver {

    private int rotationsBufferSize;

    public PlayerFactory() {

        this.reload();

    }

    public @NotNull ReactPlayer create(
            final @NotNull UUID uniqueId,
            final @NotNull String name,
            final @NotNull Player bukkitPlayer
    ) {
        return new ReactPlayer(uniqueId, name, bukkitPlayer, rotationsBufferSize);
    }

    public void reload() {

        final Config config = React.INSTANCE.getMainConfig();

        this.onReload(config);

    }

    @Override
    public void onReload(@NotNull Config config) {

        this.rotationsBufferSize = config.node("player", "data", "rotations-buffer-size").getInt(150);

    }
}
