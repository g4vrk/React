package com.g4vrk.react.alert.printer;

import com.g4vrk.react.alert.publish.Publisher;
import com.g4vrk.react.player.model.ReactPlayer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public final class AlertPrinter {

    private final Publisher<Component> publisher;

    private final Component alertFormat;

    public void print(
            final @NotNull ReactPlayer player,
            final @NotNull String checkName
    ) {
        print(player, checkName, null);
    }

    public void print(
            final @NotNull ReactPlayer player,
            final @NotNull String checkName,
            final @Nullable Component verbose
    ) {
        final Component formatted = formatAlert(alertFormat, player.getName(), checkName, verbose);

        publisher.publish(formatted);
    }

    private @NotNull Component formatAlert(
            final @NotNull Component format,
            final @NotNull String playerName,
            final @NotNull String checkName,
            final @Nullable Component verbose
    ) {
        Component component = format;

        component = replace(component, "player", Component.text(playerName));
        component = replace(component, "check", Component.text(checkName));
        component = replace(component, "verbose", verbose == null ? Component.empty() : verbose);

        return component;
    }

    private @NotNull Component replace(
            final @NotNull Component component,
            final @NotNull String placeholder,
            final @NotNull Component replacement
    ) {
        return component.replaceText(builder ->
                builder.matchLiteral('{' + placeholder + '}')
                        .replacement(replacement)
        );
    }
}
