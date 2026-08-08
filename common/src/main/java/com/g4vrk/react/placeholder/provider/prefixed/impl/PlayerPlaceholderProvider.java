package com.g4vrk.react.placeholder.provider.prefixed.impl;

import com.g4vrk.react.placeholder.provider.prefixed.PrefixedPlaceholderProvider;

public class PlayerPlaceholderProvider extends PrefixedPlaceholderProvider {

    public PlayerPlaceholderProvider() {

        super("player");

        super.replacement("name", (offlinePlayer, s) -> offlinePlayer.getName())
                .replacement("uuid", (offlinePlayer, s) -> offlinePlayer.getUniqueId().toString());

    }

}
