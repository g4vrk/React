package com.g4vrk.react.api.addon.descriptor.impl;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.functionalConfiguration.loader.ConfigLoader;
import com.g4vrk.react.api.addon.descriptor.AddonDescriptor;
import com.g4vrk.react.api.addon.meta.AddonMetadata;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import static java.util.Objects.requireNonNull;

public class SimpleAddonDescriptor implements AddonDescriptor<Path> {

    private final ConfigLoader<?> loader;

    public SimpleAddonDescriptor(
            @NotNull ConfigLoader<?> loader
    ) {
        this.loader = loader;
    }

    @Override
    public @NotNull AddonMetadata read(@NotNull Path source) throws IOException {

        final Config config = loader.from(source);

        final String name = requireNonNull(config.node("name").getString(), "Addon name cannot be null!");
        final String version = requireNonNull(config.node("version").getString(), "Addon version cannot be null!");
        final String mainClass = requireNonNull(config.node("mainClass").getString(), "Main class cannot be null!");

        final String description = config.node("description").getString();

        final Collection<String> authors = new ObjectOpenHashSet<>();
        try {
            authors.addAll(config.node("authors").getList(String.class, Collections.emptyList()));
        } catch (final Throwable ignored) {}

        final Collection<String> contributors = new ObjectOpenHashSet<>();
        try {
            contributors.addAll(config.node("contributors").getList(String.class, Collections.emptyList()));
        } catch (final Throwable ignored) {}

        return new AddonMetadata(
                name,
                version,
                mainClass,
                description,
                authors,
                contributors
        );
    }
}
