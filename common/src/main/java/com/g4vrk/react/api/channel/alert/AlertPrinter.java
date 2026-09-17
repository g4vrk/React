package com.g4vrk.react.api.channel.alert;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.player.ReactPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public final class AlertPrinter implements ReloadObserver {

    private final Consumer<Component> consumer;

    private final Function<String, Component> serializer;

    private Component alertFormat;

    public AlertPrinter(
            @NotNull Consumer<Component> consumer,
            @NotNull Function<String, Component> serializer
    ) {
        this.consumer = consumer;
        this.serializer = serializer;

        this.reload();
    }

    public void reload() {

        final Config config = React.INSTANCE.getMainConfig();

        this.onReload(config);

    }

    @Override
    public void onReload(@NotNull Config config) {

        final String alertFormatRaw = config.node("alerts", "format")
                .getString("&c«React» &7| &f{player} - {check} {verbose}");

        this.alertFormat = serializer.apply(alertFormatRaw);

    }

    public void print(
            final @NotNull ReactPlayer player,
            final @NotNull String checkName,
            final double violations
    ) {
        print(player, checkName, violations, null);
    }

    public void print(
            final @NotNull ReactPlayer player,
            final @NotNull String checkName,
            final double violations,
            final @Nullable Component verbose
    ) {
        final Component formatted = formatAlert(alertFormat, player.getName(), checkName, violations, verbose);

        consumer.accept(formatted);
    }

    private @NotNull Component formatAlert(
            final @NotNull Component format,
            final @NotNull String playerName,
            final @NotNull String checkName,
            final double violations,
            final @Nullable Component verbose
    ) {
        Component component = format;

        component = replace(component, "player", Component.text(playerName));
        component = replace(component, "check", Component.text(checkName));
        component = replace(component, "verbose", verbose == null ? Component.empty() : verbose);
        component = replace(component, "vl", Component.text(violations));
        component = replace(component, "violations", Component.text(violations));

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
