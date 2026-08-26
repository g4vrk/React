package com.g4vrk.react.storage.model;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public sealed interface StorageMutation permits StorageMutation.Violation, StorageMutation.Inference {

    @NotNull UUID playerUuid();

    record Violation(
            @NotNull UUID playerUuid,
            @NotNull String check,
            double violations,
            long updatedAt
    ) implements StorageMutation {
    }

    record Inference(
            @NotNull UUID playerUuid,
            @NotNull UUID id,
            @NotNull StoredInference value
    ) implements StorageMutation {
    }
}
