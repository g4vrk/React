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

    public InferenceHistory() {

        this.reload();

    }

    public void reload() {

        final Config config = React.INSTANCE.getHistoryConfig();

        this.onReload(config);

    }

    @Override
    public void onReload(@NotNull Config config) {

        int maxEntries = config.node("history", "inference", "entries", "max").getInt(200);

        this.entries = new Buffer<>(maxEntries, InferenceHistoryEntry[]::new);

    }

    public void add(
            final @NotNull InferenceHistoryEntry entry
    ) {
        this.entries.add(entry);
    }

    public @Nullable InferenceHistoryEntry get(
            final int index
    ) {
        if (index < 0 || index >= this.size()) {
            return null;
        }

        return this.entries.getUnsafe(index);
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