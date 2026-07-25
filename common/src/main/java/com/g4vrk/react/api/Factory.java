package com.g4vrk.react.api;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Factory<T> {

    @NotNull T create();

}
