package com.g4vrk.react.universe;

public final class UniverseData {
    private UniverseState state = new UniverseState();
    private UniverseBuffer history = new UniverseBuffer(64);
    public final UniverseCombatActivity combatActivity = new UniverseCombatActivity();
    private boolean active;

    public synchronized UniverseState activate() {
        active = true;
        return state;
    }

    public synchronized boolean isCurrent(UniverseState expected) {
        return active && state == expected;
    }

    public synchronized void deactivate() {
        if (active) {
            reset();
        }
    }

    public synchronized void reset() {
        state = new UniverseState();
        history.clear();
        combatActivity.reset();
        active = false;
    }

    public synchronized void clearHistory() {
        history.clear();
    }

    public synchronized void push(UniverseFrame frame) {
        history.addFrame(frame);
    }

    public synchronized UniverseFrame[] snapshot(int window) {
        if (history.size() < window) {
            return null;
        }
        UniverseFrame[] frames = history.getSnapshot().toArray(UniverseFrame[]::new);
        return java.util.Arrays.copyOfRange(frames, frames.length - window, frames.length);
    }

    public synchronized void resize(int size) {
        if (history.capacity() == size) {
            return;
        }
        history = new UniverseBuffer(size);
    }

    public synchronized UniverseFrame[] pollSnapshot(int step) {
        java.util.List<UniverseFrame> frames = history.pollSnapshot(step);
        return frames == null ? null : frames.toArray(UniverseFrame[]::new);
    }
}
