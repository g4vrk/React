package com.g4vrk.react.api.channel.print.impl;

import com.g4vrk.config.Config;
import com.g4vrk.react.React;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.function.Consumer;
import java.util.function.Function;

public class VerbosePrinter extends AlertPrinter {

    public VerbosePrinter(
            final @NotNull Consumer<Component> consumer,
            final @NotNull Function<String, Component> serializer
    ) {

        super(consumer, serializer);

    }

    @Override
    protected @NotNull String rawFormat() {

        final Config config = React.INSTANCE.getMainConfig();

        final ConfigurationNode formatNode = config.node("verbose", "format");

        return formatNode.getString("&c«React» &7| &f{player} - {check} {verbose}");

    }

}
