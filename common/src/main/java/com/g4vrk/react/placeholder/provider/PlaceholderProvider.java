package com.g4vrk.react.placeholder.provider;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public interface PlaceholderProvider {

    @NotNull String provide(@NotNull OfflinePlayer player, @NotNull String placeholder);

}