package com.g4vrk.react.alert.publish;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Publisher<T> {

    void publish(@NotNull T input);

}
