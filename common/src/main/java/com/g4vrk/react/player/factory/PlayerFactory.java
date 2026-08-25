package com.g4vrk.react.player.factory;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.util.platform.PlayerPlatformUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PlayerFactory implements ReloadObserver {

    private int rotationsBufferSize;
    private boolean checkBedrockPlayers;

    public PlayerFactory() {

        this.reload();

    }

    public @Nullable ReactPlayer create(
            final @NotNull UUID uniqueId,
            final @NotNull String name,
            final @NotNull Player bukkitPlayer
    ) {
        if (!checkBedrockPlayers && PlayerPlatformUtil.bedrockPlayer(uniqueId)) {
            return null;
        }

        return new ReactPlayer(uniqueId, name, bukkitPlayer, rotationsBufferSize);
    }

    public void reload() {

        final Config config = React.INSTANCE.getMainConfig();

        this.onReload(config);

    }

    @Override
    public void onReload(@NotNull Config config) {

        this.rotationsBufferSize = config.node("player", "data", "rotations-buffer-size").getInt(150);
        this.checkBedrockPlayers = config.node("player", "check-bedrock-players").getBoolean(false);

    }
}
