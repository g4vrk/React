package com.g4vrk.react.alert.printer;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.alert.publish.Publisher;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.player.ReactPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class AlertPrinter implements ReloadObserver {

    private final Publisher<Component> publisher;

    private final Function<String, Component> serializer;

    private Component alertFormat;

    public AlertPrinter(
            @NotNull Publisher<Component> publisher,
            @NotNull Function<String, Component> serializer
    ) {
        this.publisher = publisher;
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
