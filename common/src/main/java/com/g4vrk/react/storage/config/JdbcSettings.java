package com.g4vrk.react.storage.config;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record JdbcSettings(
        @NotNull String jdbcUrl,
        @NotNull String driverClassName,
        @NotNull String username,
        @NotNull String password,
        @NotNull PoolSettings pool,
        @NotNull Map<String, String> properties
) {
}
