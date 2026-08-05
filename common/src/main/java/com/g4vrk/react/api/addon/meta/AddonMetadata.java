package com.g4vrk.react.api.addon.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public record AddonMetadata(
        @NotNull String name,
        @NotNull String version,
        @NotNull String mainClass,
        @Nullable String description,
        @Nullable Collection<String> authors,
        @Nullable Collection<String> contributors
) {}