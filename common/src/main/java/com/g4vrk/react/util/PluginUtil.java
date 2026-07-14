package com.g4vrk.react.util;

import lombok.experimental.UtilityClass;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import static org.bukkit.Bukkit.getServer;

@UtilityClass
public class PluginUtil {

    private PluginManager pluginManager;

    private @NotNull PluginManager getPluginManager() {
        if (pluginManager != null)
            return pluginManager;

        return pluginManager = getServer().getPluginManager();
    }

    public boolean containsPlugin(
            final @NotNull String pluginName
    ) {
        return getPluginManager().getPlugin(pluginName) != null;
    }

    public boolean containsAll(
            final @NotNull String @NotNull ... pluginNames
    ) {
        for (final String pluginName : pluginNames) {
            if (!containsPlugin(pluginName)) {
                return false;
            }
        }
        return true;
    }
}
