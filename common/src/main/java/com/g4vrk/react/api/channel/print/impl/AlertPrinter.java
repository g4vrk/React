package com.g4vrk.react.api.channel.print.impl;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.player.ReactPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.function.Consumer;
import java.util.function.Function;

public class AlertPrinter extends ConfigurablePrinter {

    public AlertPrinter(
            final @NotNull Consumer<Component> consumer,
            final @NotNull Function<String, Component> serializer
    ) {

        super(consumer, serializer);

        this.reload();

    }

    @Override
    protected @NotNull String rawFormat() {

        final Config config = React.INSTANCE.getMainConfig();

        final ConfigurationNode formatNode = config.node("alerts", "format");

        return formatNode.getString("&c«React» &7| &f{player} - {check} {verbose}");

    }

    public void print(
            final @NotNull ReactPlayer player,
            final @NotNull String check,
            final double violations,
            final @Nullable Component verbose
    ) {

        Component result = this.template();

        result = replace(result, "player", Component.text(player.getName()));
        result = replace(result, "check", Component.text(check));
        result = replace(result, "verbose", verbose == null ? Component.empty() : verbose);
        result = replace(result, "vl", Component.text(violations));
        result = replace(result, "violations", Component.text(violations));

        this.publish(result);

    }

}
