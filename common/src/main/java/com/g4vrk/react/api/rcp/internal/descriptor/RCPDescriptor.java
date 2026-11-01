package com.g4vrk.react.api.rcp.internal.descriptor;

import com.g4vrk.config.Config;
import com.g4vrk.config.loader.ConfigLoader;
import com.g4vrk.react.api.Descriptor;
import com.g4vrk.react.api.rcp.meta.RCPMetadata;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public final class RCPDescriptor implements Descriptor<RCPMetadata, Path> {

    private final ConfigLoader<?> loader;

    public RCPDescriptor(
            final @NotNull ConfigLoader<?> loader
    ) {
        this.loader = requireNonNull(loader, "loader");
    }

    @Override
    public @NotNull RCPMetadata read(@NotNull final Path source) throws IOException {

        final Config config = loader.from(source);

        final String name = requireNonNull(config.node("name").getString(), "RCP name cannot be null!");
        final String version = requireNonNull(config.node("version").getString(), "RCP version cannot be null!");
        final String mainClass = requireNonNull(config.node("mainClass").getString(), "Main class cannot be null!");

        return new RCPMetadata(
                name,
                version,
                mainClass
        );
    }
}