package com.g4vrk.react.log;

import com.g4vrk.react.React;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class FastLog {
    
    @Setter
    private boolean debugEnabled = false;
    private final Logger LOGGER = React.LOGGER;

    public void log(@NotNull LogLevel level, @NotNull String message, @Nullable Throwable t) {
        switch (level) {
            case INFO -> {
                if (t != null) {
                    LOGGER.info(message, t);
                    return;
                }

                LOGGER.info(message);
            }
            case WARN -> {
                if (t != null) {
                    LOGGER.warn(message, t);
                    return;
                }

                LOGGER.warn(message);
            }
            case ERROR -> {
                if (t != null) {
                    LOGGER.error(message, t);
                    return;
                }

                LOGGER.error(message);
            }
            case DEBUG -> {
                if (!debugEnabled) return;

                if (t != null) {
                    LOGGER.info(message, t);
                    return;
                }

                LOGGER.info(message);
            }
        }
    }

    public void log(@NotNull LogLevel level, @NotNull String message) {
        log(level, message, null);
    }

    public @NotNull Logger newLogger(@NotNull String prefix) {
        return LoggerFactory.getLogger(LOGGER.getName() + "#" + prefix);
    }
}
