package com.g4vrk.react.api;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Descriptor<M extends Metadata, S> {

    @NotNull M read(final @NotNull S source) throws Exception;

}
