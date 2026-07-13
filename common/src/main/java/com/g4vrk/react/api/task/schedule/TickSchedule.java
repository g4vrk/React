package com.g4vrk.react.api.task.schedule;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@Getter @ToString
@EqualsAndHashCode
public final class TickSchedule {

    private static final TickSchedule INSTANT = new TickSchedule(0, 0);

    private final long delay;
    private final long period;

    private TickSchedule(
            long delay,
            long period
    ) {
        this.delay = delay;
        this.period = period;
    }

    public static @NotNull TickSchedule instant() {
        return INSTANT;
    }

    public static @NotNull TickSchedule delayed(
            final long ticks
    ) {
        return new TickSchedule(Math.max(0, ticks), 0);
    }

    public static @NotNull TickSchedule repeating(
            final long delay,
            final long period
    ) {
        return new TickSchedule(Math.max(0, delay), Math.max(0, period));
    }

    public static @NotNull TickSchedule fixedRate(
            final long period
    ) {
        return new TickSchedule(0, Math.max(0, period));
    }

    public boolean isDelayed() {
        return delay > 0;
    }

    public boolean isRepeating() {
        return period > 0;
    }
}