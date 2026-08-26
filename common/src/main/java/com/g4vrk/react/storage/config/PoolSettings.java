package com.g4vrk.react.storage.config;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public record PoolSettings(
        int maximumPoolSize,
        int minimumIdle,
        @NotNull Duration connectionTimeout,
        @NotNull Duration validationTimeout,
        @NotNull Duration idleTimeout,
        @NotNull Duration maxLifetime
) {
}
