package com.g4vrk.react.api;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Loader<R, S> {

    @NotNull R load(final @NotNull S source) throws Exception;

}
