package com.g4vrk.react.api.channel.print;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class Printer<R> {

    private final Consumer<R> consumer;

    protected Printer(
            final @NotNull Consumer<R> consumer
    ) {

        this.consumer = consumer;

    }

    protected final void publish(
            final @NotNull R result
    ) {

        this.consumer.accept(result);

    }

}