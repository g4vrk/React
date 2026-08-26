package com.g4vrk.react.storage.config;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum DatabaseType {
    H2,
    MYSQL,
    MONGODB,
    SQLITE;

    public static @NotNull DatabaseType parse(final String input) {
        if (input == null) {
            return SQLITE;
        }

        try {
            return valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return SQLITE;
        }
    }

    public @NotNull String configName() {
        return name().toLowerCase(Locale.ROOT) + ".yml";
    }
}
