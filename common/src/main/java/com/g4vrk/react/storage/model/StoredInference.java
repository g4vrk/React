package com.g4vrk.react.storage.model;

import org.jetbrains.annotations.NotNull;

public record StoredInference(
        long timestamp,
        @NotNull String check,
        double probability,
        double confidence
) {
}
