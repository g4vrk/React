package com.g4vrk.react.history;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.history.entry.HistoryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;

public final class FailureHistory {

    private final ArrayDeque<HistoryEntry> entries = new ArrayDeque<>();

    private final int maxEntries;

    public FailureHistory() {
        final Config historyConfig = React.INSTANCE.getHistoryConfig();

        this.maxEntries = historyConfig.node("entries", "max").getInt(200);
    }

    public synchronized void add(
            final @NotNull HistoryEntry entry
    ) {

        if (this.entries.size() >= maxEntries) {
            this.entries.removeFirst();
        }

        this.entries.addLast(entry);

    }

    public synchronized @NotNull List<HistoryEntry> getEntries() {
        return List.copyOf(this.entries);
    }

    public synchronized @Nullable HistoryEntry getLatest() {
        return this.entries.peekLast();
    }

    public synchronized int size() {
        return this.entries.size();
    }

    public synchronized void clear() {
        this.entries.clear();
    }

}