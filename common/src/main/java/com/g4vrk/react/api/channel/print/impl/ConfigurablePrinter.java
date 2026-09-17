package com.g4vrk.react.api.channel.print.impl;

import com.g4vrk.react.api.channel.print.Printer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ConfigurablePrinter extends Printer<Component> {

    private final Function<String, Component> serializer;

    private Component template;

    protected ConfigurablePrinter(
            final @NotNull Consumer<Component> consumer,
            final @NotNull Function<String, Component> serializer
    ) {
        super(consumer);

        this.serializer = serializer;
    }

    protected abstract @NotNull String rawFormat();

    public final void reload() {

        this.template = serializer.apply(this.rawFormat());

    }

    protected final @NotNull Component template() {
        return this.template;
    }

    protected final @NotNull Component replace(
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
