package com.g4vrk.reactdataset.storage;

import com.g4vrk.reactdataset.session.RecordingSession;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** One ordered IO worker; completed batches outlive their recording sessions. */
public final class UniverseBatchWriter implements AutoCloseable {
    private final CsvDatasetStorage storage;
    private final ArrayDeque<Write> writes = new ArrayDeque<>();
    private final ScheduledExecutorService worker;
    private long sequence = System.currentTimeMillis() * 1000L;
    private boolean closed;

    public UniverseBatchWriter(CsvDatasetStorage storage) {
        this.storage = storage;
        try {
            for (Path journal : storage.universeRecovery()) {
                writes.add(new Write(journal));
                try {
                    sequence = Math.max(sequence, Long.parseLong(journal.getFileName().toString().split("-", 2)[0]));
                } catch (NumberFormatException legacy) {
                    // Direct storage API journals use a UUID, rather than a worker sequence.
                }
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read completed UNIVERSE batches", failure);
        }
        worker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "React-UNIVERSE-datasets");
            thread.setDaemon(true);
            return thread;
        });
        worker.scheduleWithFixedDelay(this::drain, 0L, 1L, TimeUnit.SECONDS);
    }

    public synchronized void enqueue(RecordingSession session, RecordingSession.CapturedFile batch,
                                     BiConsumer<Path, Path> saved, Consumer<Exception> failed) {
        if (closed) throw new IllegalStateException("UNIVERSE writer is closed");
        String order = String.format(java.util.Locale.ROOT, "%019d", ++sequence);
        UUID key = UUID.randomUUID();
        String id = order + "-" + key;
        Path csv = storage.pathForRecording(session.safePlayerName(), session.source(),
                session.recordingDate(), session.recordingId());
        writes.add(new Write(session, batch, id, csv,
                session.safePlayerName() + "-" + new java.math.BigInteger(key.toString().replace("-", ""), 16)
                        + "-" + session.label().fileLabel() + ".csv", saved, failed));
        worker.execute(this::drain);
    }

    private void drain() {
        ArrayList<Write> pending;
        synchronized (this) { pending = new ArrayList<>(writes); }
        // Stage every complete batch even if an earlier destination cannot be written.
        for (Write write : pending) {
            if (write.journal != null) continue;
            try {
                write.journal = storage.stageUniverseBatch(write.id, write.csv, write.pendingName,
                        write.session.label(), write.batch.frames());
            } catch (Exception failure) { report(write, failure); }
        }
        while (true) {
            Write write;
            synchronized (this) { write = writes.peek(); }
            if (write == null || write.journal == null) return;
            CsvDatasetStorage.SavedBatch result;
            try {
                result = storage.commitUniverseBatch(write.journal);
            } catch (Exception failure) {
                report(write, failure);
                return;
            }
            synchronized (this) { writes.removeFirst(); }
            if (write.saved != null) {
                try { write.saved.accept(result.csv(), result.pending()); }
                catch (RuntimeException failure) { storage.logger().error("UNIVERSE batch saved, but notification failed", failure); }
            }
        }
    }

    private void report(Write write, Exception failure) {
        if (write.reported) return;
        write.reported = true;
        storage.logger().error("UNIVERSE batch retained for retry: {}", write.journal == null ? write.id : write.journal, failure);
        if (write.failed != null) {
            try { write.failed.accept(failure); }
            catch (RuntimeException notification) { storage.logger().warn("Could not report UNIVERSE write failure", notification); }
        }
    }

    @Override public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
            worker.execute(this::drain);
            worker.shutdown();
        }
        try {
            if (!worker.awaitTermination(30L, TimeUnit.SECONDS)) {
                throw new IllegalStateException("UNIVERSE batch writer is still saving completed batches");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while saving completed UNIVERSE batches", interrupted);
        }
        synchronized (this) {
            if (writes.stream().anyMatch(write -> write.journal == null)) {
                throw new IllegalStateException("Unable to persist completed UNIVERSE batches; check the dataset directory");
            }
        }
    }

    private static final class Write {
        Path journal;
        RecordingSession session;
        RecordingSession.CapturedFile batch;
        String id, pendingName;
        Path csv;
        BiConsumer<Path, Path> saved;
        Consumer<Exception> failed;
        boolean reported;

        Write(Path journal) { this.journal = journal; }
        Write(RecordingSession session, RecordingSession.CapturedFile batch, String id, Path csv,
              String pendingName, BiConsumer<Path, Path> saved, Consumer<Exception> failed) {
            this.session = session; this.batch = batch; this.id = id; this.csv = csv;
            this.pendingName = pendingName; this.saved = saved; this.failed = failed;
        }
    }
}
