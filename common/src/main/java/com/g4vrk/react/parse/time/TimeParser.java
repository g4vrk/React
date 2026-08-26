package com.g4vrk.react.parse.time;

import com.g4vrk.react.React;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TimeParser {

    private final Pattern PATTERN = Pattern.compile("^(\\d+)(ms|[smhd])$");

    public @NotNull TimeValue parse(final @NotNull String input) {
        final Matcher matcher = PATTERN.matcher(input.trim().toLowerCase());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time: " + input);
        }

        final long value = Long.parseLong(matcher.group(1));
        final TimeUnit unit = switch (matcher.group(2)) {
            case "ms" -> TimeUnit.MILLISECONDS;
            case "s" -> TimeUnit.SECONDS;
            case "m" -> TimeUnit.MINUTES;
            case "h" -> TimeUnit.HOURS;
            case "d" -> TimeUnit.DAYS;
            default -> throw new IllegalStateException("Unexpected unit: " + matcher.group(2));
        };

        return new TimeValue(value, unit);
    }

    public @NotNull Duration parseDuration(final @NotNull String input) {
        final TimeValue value = parse(input);
        return Duration.ofMillis(value.toMillis());
    }

    public @NotNull TimeValue parseOrDefault(
            final @Nullable String input,
            final @NotNull TimeValue defaultValue
    ) {
        if (input == null) {
            return defaultValue;
        }

        try {
            return parse(input);
        } catch (final IllegalArgumentException ex) {
            logger().warn(
                    "Invalid time value '{}' in the configuration, using default: {} {}",
                    input, defaultValue.value(), defaultValue.unit()
            );
            return defaultValue;
        }
    }

    public @NotNull Duration parseDurationOrDefault(
            final @Nullable String input,
            final @NotNull Duration defaultValue
    ) {
        if (input == null) {
            return defaultValue;
        }

        try {
            return parseDuration(input);
        } catch (final IllegalArgumentException ex) {
            logger().warn(
                    "Invalid time value '{}' in the configuration, using default: {}",
                    input, defaultValue
            );
            return defaultValue;
        }
    }

    private static @NotNull Logger logger() {
        return React.INSTANCE.getLogger();
    }
}
