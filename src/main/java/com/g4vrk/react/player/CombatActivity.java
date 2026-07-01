package com.g4vrk.react.player;

import lombok.Getter;

@Getter
public final class CombatActivity {

    private volatile long activityUntil = 0L;

    public boolean isActive() {
        return System.currentTimeMillis() < activityUntil;
    }

    public void extend(
            final long durationTicks
    ) {
        this.activityUntil = System.currentTimeMillis() + durationTicks * 50L;
    }

    public void clear() {
        this.activityUntil = 0L;
    }
}