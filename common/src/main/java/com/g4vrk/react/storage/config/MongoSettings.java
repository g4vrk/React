package com.g4vrk.react.storage.config;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public record MongoSettings(
        @NotNull String uri,
        @NotNull String host,
        int port,
        @NotNull String database,
        @NotNull String username,
        @NotNull String password,
        @NotNull String authenticationDatabase,
        boolean tls,
        boolean retryWrites,
        int minimumPoolSize,
        int maximumPoolSize,
        @NotNull Duration connectTimeout,
        @NotNull Duration socketTimeout,
        @NotNull Duration serverSelectionTimeout,
        @NotNull Duration maxConnectionIdleTime,
        @NotNull Duration maxConnectionLifeTime,
        @NotNull String applicationName
) {
}
