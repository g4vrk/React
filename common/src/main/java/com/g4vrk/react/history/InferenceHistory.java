package com.g4vrk.react.history;

import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public final class InferenceHistory {

    private final List<InferenceHistoryEntry> entries = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final long retentionMillis;
    private final Consumer<InferenceHistoryEntry> entrySink;

    public InferenceHistory(final @NotNull Duration retention) {
        this(retention, ignored -> {
        });
    }

    public InferenceHistory(
            final @NotNull Duration retention,
            final @NotNull Consumer<InferenceHistoryEntry> entrySink
    ) {
        this.retentionMillis = retention.toMillis();
        this.entrySink = entrySink;
    }

    public void add(
            final @NotNull InferenceHistoryEntry entry
    ) {
        lock.writeLock().lock();
        try {
            pruneUnsafe(System.currentTimeMillis() - retentionMillis);
            entries.add(entry);
        } finally {
            lock.writeLock().unlock();
        }
        entrySink.accept(entry);
    }

    public void replace(final @NotNull Collection<InferenceHistoryEntry> loaded) {
        lock.writeLock().lock();
        try {
            entries.clear();
            entries.addAll(loaded);
            entries.sort(Comparator.comparingLong(InferenceHistoryEntry::getTimestamp));
            pruneUnsafe(System.currentTimeMillis() - retentionMillis);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void merge(final @NotNull Collection<InferenceHistoryEntry> loaded) {
        lock.writeLock().lock();
        try {
            final Set<EntryIdentity> known = new HashSet<>(entries.size() + loaded.size());
            for (final InferenceHistoryEntry entry : entries) {
                known.add(EntryIdentity.of(entry));
            }
            for (final InferenceHistoryEntry entry : loaded) {
                if (known.add(EntryIdentity.of(entry))) {
                    entries.add(entry);
                }
            }
            entries.sort(Comparator.comparingLong(InferenceHistoryEntry::getTimestamp));
            pruneUnsafe(System.currentTimeMillis() - retentionMillis);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void pruneBefore(final long cutoff) {
        lock.writeLock().lock();
        try {
            pruneUnsafe(cutoff);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Nullable InferenceHistoryEntry get(
            final int index
    ) {
        pruneExpired();
        lock.readLock().lock();
        try {
            return index < 0 || index >= entries.size() ? null : entries.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }

    public @NotNull InferenceHistoryEntry[] entries() {
        pruneExpired();
        lock.readLock().lock();
        try {
            return entries.toArray(InferenceHistoryEntry[]::new);
        } finally {
            lock.readLock().unlock();
        }
    }

    public @Nullable InferenceHistoryEntry latest() {
        pruneExpired();
        lock.readLock().lock();
        try {
            return entries.isEmpty() ? null : entries.get(entries.size() - 1);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        pruneExpired();
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            entries.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void pruneUnsafe(final long cutoff) {
        entries.removeIf(entry -> entry.getTimestamp() < cutoff);
    }

    private void pruneExpired() {
        pruneBefore(System.currentTimeMillis() - retentionMillis);
    }

    private record EntryIdentity(long timestamp, String check, long probability, long confidence) {
        private static @NotNull EntryIdentity of(final @NotNull InferenceHistoryEntry entry) {
            return new EntryIdentity(
                    entry.getTimestamp(),
                    entry.getCheck().getConfigId(),
                    Double.doubleToLongBits(entry.getProbability()),
                    Double.doubleToLongBits(entry.getConfidence())
            );
        }
    }
}
