package com.g4vrk.react.parse.time;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TimeParser {

    private final Pattern PATTERN = Pattern.compile("^(\\d+)([smhd])$");

    public @NotNull TimeValue parse(final @NotNull String input) {
        final Matcher matcher = PATTERN.matcher(input.toLowerCase());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time: " + input);
        }

        final long value = Long.parseLong(matcher.group(1));
        final TimeUnit unit = switch (matcher.group(2)) {
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
}