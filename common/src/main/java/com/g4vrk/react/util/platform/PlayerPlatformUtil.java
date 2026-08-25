package com.g4vrk.react.util.platform;

import lombok.experimental.UtilityClass;
import org.geysermc.api.Geyser;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@UtilityClass
public class PlayerPlatformUtil {

    private final boolean HAS_FLOODGATE = hasClass("org.geysermc.floodgate.api.FloodgateApi");
    private final boolean HAS_GEYSER = hasClass("org.geysermc.api.Geyser");

    public boolean bedrockPlayer(
            final @NotNull UUID uuid
    ) {
        return HAS_FLOODGATE && FloodgateApi.getInstance().isFloodgatePlayer(uuid)
                || HAS_GEYSER && Geyser.api().isBedrockPlayer(uuid);
    }

    private boolean hasClass(final @NotNull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }
}
