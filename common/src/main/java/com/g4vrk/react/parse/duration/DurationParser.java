package com.g4vrk.react.parse.duration;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class DurationParser {

    private final Pattern PATTERN = Pattern.compile("^(\\d+)([smhd])$");

    public @NotNull Duration parse(
            final @NotNull String input
    ) {
        final Matcher matcher = PATTERN.matcher(input.toLowerCase());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration: " + input);
        }

        final long value = Long.parseLong(matcher.group(1));
        final String unit = matcher.group(2);

        return switch (unit) {
            case "s" -> Duration.ofSeconds(value);
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            case "d" -> Duration.ofDays(value);
            default -> throw new IllegalStateException("Unexpected unit: " + unit);
        };
    }
}