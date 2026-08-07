package com.g4vrk.react.history;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;

public final class InferenceHistory implements ReloadObserver {

    private final ArrayDeque<InferenceHistoryEntry> entries = new ArrayDeque<>();

    private int maxEntries;

    public InferenceHistory() {

        this.reload();

    }

    public void reload() {

        final Config config = React.INSTANCE.getHistoryConfig();

        this.onReload(config);

    }

    @Override
    public void onReload(@NotNull Config config) {

        this.maxEntries = config.node("history", "entries", "max").getInt(200);

    }

    public synchronized void add(
            final @NotNull InferenceHistoryEntry entry
    ) {

        if (this.entries.size() >= maxEntries) {
            this.entries.removeFirst();
        }

        this.entries.addLast(entry);

    }

    public synchronized @NotNull List<InferenceHistoryEntry> getEntries() {
        return List.copyOf(this.entries);
    }

    public synchronized @Nullable InferenceHistoryEntry getLatest() {
        return this.entries.peekLast();
    }

    public synchronized int size() {
        return this.entries.size();
    }

    public synchronized void clear() {
        this.entries.clear();
    }
}