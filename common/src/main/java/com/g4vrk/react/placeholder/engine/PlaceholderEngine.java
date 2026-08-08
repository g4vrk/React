package com.g4vrk.react.placeholder.engine;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public interface PlaceholderEngine {

    @NotNull String process(
            @NotNull OfflinePlayer player,
            @NotNull String text
    );

    @NotNull Component process(
            @NotNull OfflinePlayer player,
            @NotNull Component text
    );

    default @NotNull PlaceholderEngine with(
            @NotNull PlaceholderEngine other
    ) {
        return new PlaceholderEngine() {

            @Override
            public @NotNull String process(
                    @NotNull OfflinePlayer player,
                    @NotNull String text
            ) {
                return other.process(
                        player,
                        PlaceholderEngine.this.process(player, text)
                );
            }

            @Override
            public @NotNull Component process(
                    @NotNull OfflinePlayer player,
                    @NotNull Component text
            ) {
                return other.process(
                        player,
                        PlaceholderEngine.this.process(player, text)
                );
            }
        };
    }
}