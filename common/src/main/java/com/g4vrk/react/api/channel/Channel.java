package com.g4vrk.react.api.channel;

import org.jetbrains.annotations.NotNull;

public interface Channel<I> {

    void publish(final @NotNull I input);

}
