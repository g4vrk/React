package com.g4vrk.react.util.placeholder;

import com.g4vrk.react.util.PluginUtil;
import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class PlaceholderAPIUtil {

    private static final String PLACEHOLDER_API = "PlaceholderAPI";

    private static final boolean PRESENT =
            PluginUtil.containsPlugin(PLACEHOLDER_API);

    public boolean apiPresent() {
        return PRESENT;
    }

    public @NotNull String setPlaceholders(
            final @NotNull OfflinePlayer player,
            final @NotNull String text
    ) {

        if (!PRESENT) {
            return text;
        }

        return PlaceholderAPI.setPlaceholders(player, text);

    }

    public @NotNull String setBracketPlaceholders(
            final @NotNull OfflinePlayer player,
            final @NotNull String text
    ) {

        if (!PRESENT) {
            return text;
        }

        return PlaceholderAPI.setBracketPlaceholders(player, text);

    }

}