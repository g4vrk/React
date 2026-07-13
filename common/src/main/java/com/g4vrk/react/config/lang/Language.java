package com.g4vrk.react.config.lang;

import org.jetbrains.annotations.NotNull;

public enum Language {
    EN,
    RU;

    public static @NotNull Language resolve() {
        try {
            final String languageStr = System.getProperty("user.language").toUpperCase();

            return Language.valueOf(languageStr);
        } catch (final Throwable ignored) {
            return RU;
        }
    }
}