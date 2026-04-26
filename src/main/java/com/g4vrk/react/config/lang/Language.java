package com.g4vrk.react.config.lang;

import com.g4vrk.react.log.FastLog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public enum Language {
    EN,
    RU;

    private static final Logger LOGGER = FastLog.newLogger("Language");

    public static @NotNull Language resolve() {
        try {
            final String languageStr = System.getProperty("user.language").toUpperCase();
            final Language language = Language.valueOf(languageStr);

            LOGGER.info("Language successfully resolved, selected language: {}", languageStr);

            return language;
        } catch (Exception ignored) {
            return EN;
        }
    }
}