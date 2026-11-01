package com.g4vrk.react.api.rcp.meta;

import com.g4vrk.react.api.Metadata;
import org.jetbrains.annotations.NotNull;

// so simple...
public record RCPMetadata(
        @NotNull String name,
        @NotNull String version,
        @NotNull String mainClass
) implements Metadata {}
