package com.g4vrk.react.universe;

/** UNIVERSE combat deadline in milliseconds; the deadline itself is still active. */
public final class UniverseCombatActivity {
    private volatile long activityUntil;
    private volatile boolean active;

    public boolean isActive() {
        if (System.currentTimeMillis() > activityUntil) {
            active = false;
        }
        return active;
    }

    public void extend(long durationTicks) {
        activityUntil = System.currentTimeMillis() + durationTicks * 50L;
        active = true;
    }

    public void reset() {
        activityUntil = 0L;
        active = false;
    }
}
