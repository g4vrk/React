package com.g4vrk.react.history;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.buffer.Buffer;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InferenceHistory implements ReloadObserver {

    private Buffer<InferenceHistoryEntry> entries;

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

        this.maxEntries = config.node("history", "inference", "entries", "max").getInt(200);

        this.entries = new Buffer<>(maxEntries, InferenceHistoryEntry[]::new);

    }

    public void add(
            final @NotNull InferenceHistoryEntry entry
    ) {
        this.entries.add(entry);
    }

    public @NotNull InferenceHistoryEntry[] entries() {
        return this.entries.snapshot();
    }

    public @Nullable InferenceHistoryEntry latest() {
        if (this.size() == 0) {
            return null;
        }

        return this.entries.getUnsafe(this.size() - 1);
    }

    public int size() {
        return this.entries.size();
    }

    public void clear() {
        this.entries.clear();
    }
}