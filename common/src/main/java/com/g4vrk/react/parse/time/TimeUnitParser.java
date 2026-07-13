package com.g4vrk.react.parse.time;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUnitParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)([smhd])$");

    public static @NotNull TimeValue parse(
            final @NotNull String input
    ) {
        final Matcher matcher = PATTERN.matcher(input.toLowerCase());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time: " + input);
        }

        final long value = Long.parseLong(matcher.group(1));
        final String unit = matcher.group(2);

        final TimeUnit timeUnit = switch (unit) {
            case "s" -> TimeUnit.SECONDS;
            case "m" -> TimeUnit.MINUTES;
            case "h" -> TimeUnit.HOURS;
            case "d" -> TimeUnit.DAYS;
            default -> throw new IllegalStateException("Unexpected unit: " + unit);
        };

        return new TimeValue(value, timeUnit);
    }
}