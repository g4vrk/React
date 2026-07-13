package com.g4vrk.react.parse.time;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public record TimeValue(
        long value,
        @NotNull TimeUnit unit
) {

    public long toMillis() {
        return unit.toMillis(value);
    }

    public long toSeconds() {
        return unit.toSeconds(value);
    }

    public long toMinutes() {
        return unit.toMinutes(value);
    }

    public long toHours() {
        return unit.toHours(value);
    }

    public long toDays() {
        return unit.toDays(value);
    }
}