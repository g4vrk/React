package com.g4vrk.react.ml.auth.type;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum AuthType {

    NONE,
    BEARER,
    HEADER,
    QUERY,
    BODY;

    public static @NotNull AuthType safelyMatch(
            final String raw,
            final @NotNull AuthType fallback
    ) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return AuthType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return fallback;
        }
    }
}
