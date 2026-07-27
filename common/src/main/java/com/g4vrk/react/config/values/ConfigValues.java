package com.g4vrk.react.config.values;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

@Getter
public final class ConfigValues {

    @Getter(AccessLevel.NONE)
    private final ConfigurationNode root;

    private boolean debugEnabled;
    private int bufferSize;

    public ConfigValues(
            @NotNull ConfigurationNode root
    ) {
        this.root = root;

        this.setup();
    }

    private void setup() {
        this.debugEnabled = root.node("debug").getBoolean(false);

        final ConfigurationNode bufferNode = root.node("player", "data", "rotations-buffer-size");

        this.bufferSize = !bufferNode.virtual()
                ? bufferNode.getInt(150)
                : root.node("ml-check", "buffer-size").getInt(150);
    }
}
