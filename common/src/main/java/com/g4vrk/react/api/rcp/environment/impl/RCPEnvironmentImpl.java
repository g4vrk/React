package com.g4vrk.react.api.rcp.environment.impl;

import com.g4vrk.react.api.rcp.environment.RCPEnvironment;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

public record RCPEnvironmentImpl(
        @NotNull Path directory
) implements RCPEnvironment {

    public RCPEnvironmentImpl(
            final @NotNull Path directory
    ) {
        this.directory = Objects.requireNonNull(
                directory,
                "directory can't be null"
        ).toAbsolutePath().normalize();
    }

}
