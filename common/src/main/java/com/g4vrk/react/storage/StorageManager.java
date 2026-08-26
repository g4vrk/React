package com.g4vrk.react.storage;

import com.g4vrk.react.check.Check;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.storage.backend.StorageBackend;
import com.g4vrk.react.storage.backend.StorageBackendFactory;
import com.g4vrk.react.storage.config.DatabaseSettings;
import com.g4vrk.react.storage.model.PlayerStorageData;
import com.g4vrk.react.storage.model.StorageMutation;
import com.g4vrk.react.storage.model.StoredInference;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static com.g4vrk.react.storage.constant.StorageConstants.FAILURE_LOG_INTERVAL_MILLIS;

public final class StorageManager {

    private final Logger logger;
    private final DatabaseSettings settings;
    private final Function<DatabaseSettings, StorageBackend> backendFactory;

    private final ThreadPoolExecutor executor;
    private final ScheduledThreadPoolExecutor scheduler;
    private final java.util.concurrent.ConcurrentLinkedDeque<StorageMutation> pendingWrites = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private final AtomicInteger pendingWriteCount = new AtomicInteger();
    private final AtomicBoolean available = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicBoolean flushRunning = new AtomicBoolean();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private final AtomicLong reconnectDelayMillis;
    private final AtomicLong lastFailureLog = new AtomicLong();
    private final ReentrantReadWriteLock backendLock = new ReentrantReadWriteLock();
    private final ReentrantLock flushLock = new ReentrantLock();
    private final Object availabilityMonitor = new Object();

    private volatile StorageBackend backend;
    private volatile CompletableFuture<Void> initialization = CompletableFuture.completedFuture(null);
    private volatile CompletableFuture<Void> availabilitySignal;

    public StorageManager(
            final @NotNull Logger logger,
            final @NotNull DatabaseSettings settings
    ) {
        this(logger, settings, new StorageBackendFactory()::create);
    }

    StorageManager(
            final @NotNull Logger logger,
            final @NotNull DatabaseSettings settings,
            final @NotNull Function<DatabaseSettings, StorageBackend> backendFactory
    ) {
        this.logger = logger;
        this.settings = settings;
        this.backendFactory = backendFactory;
        this.availabilitySignal = settings.enabled()
                ? new CompletableFuture<>()
                : CompletableFuture.completedFuture(null);
        this.reconnectDelayMillis = new AtomicLong(settings.retryInitialDelay().toMillis());
        this.executor = new ThreadPoolExecutor(
                settings.executorThreads(),
                settings.executorThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(settings.executorQueueCapacity()),
                threadFactory("React-Database-Worker"),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.scheduler = new ScheduledThreadPoolExecutor(1, threadFactory("React-Database-Scheduler"));
        this.scheduler.setRemoveOnCancelPolicy(true);
    }

    public @NotNull CompletableFuture<Void> start() {
        if (!started.compareAndSet(false, true) || !settings.enabled()) {
            return initialization;
        }

        initialization = runAsync(this::connect);

        final long flushMillis = settings.flushInterval().toMillis();
        scheduler.scheduleWithFixedDelay(this::requestFlush, flushMillis, flushMillis, TimeUnit.MILLISECONDS);

        final long cleanupMillis = settings.cleanupInterval().toMillis();
        scheduler.scheduleWithFixedDelay(this::requestCleanup, cleanupMillis, cleanupMillis, TimeUnit.MILLISECONDS);

        initialization.whenComplete((ignored, failure) -> {
            if (failure != null) {
                logFailure("Could not initialize " + settings.type() + " storage; continuing in fail-open mode", failure);
                scheduleReconnect();
            } else {
                logger.info("{} storage initialized successfully", settings.type());
            }
        });
        return initialization;
    }

    public @NotNull CompletableFuture<PlayerStorageData> loadPlayer(final @NotNull UUID playerUuid) {
        if (!settings.enabled()) {
            return CompletableFuture.completedFuture(PlayerStorageData.EMPTY);
        }

        final CompletableFuture<Void> readiness;
        if (available.get()) {
            readiness = CompletableFuture.completedFuture(null);
        } else if (!initialization.isDone()) {
            readiness = initialization;
        } else {
            return CompletableFuture.failedFuture(new IllegalStateException("Database is unavailable"));
        }

        return readiness.thenCompose(ignored -> supplyAsync(() -> withBackend(storage ->
                storage.loadPlayer(playerUuid, historyCutoff())
        ))).whenComplete((ignored, failure) -> {
            if (failure != null && backendFailure(failure)) {
                markUnavailable(failure);
            }
        });
    }

    public @NotNull CompletableFuture<PlayerStorageData> loadPlayerWhenAvailable(final @NotNull UUID playerUuid) {
        if (closing.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Database storage is closed"));
        }
        return whenAvailable()
                .thenCompose(ignored -> loadPlayer(playerUuid))
                .handle((data, failure) -> {
                    if (failure == null) {
                        return CompletableFuture.completedFuture(data);
                    }
                    if (closing.get()) {
                        return CompletableFuture.<PlayerStorageData>failedFuture(failure);
                    }
                    return retryDelay().thenCompose(ignored -> loadPlayerWhenAvailable(playerUuid));
                })
                .thenCompose(future -> future);
    }

    public void saveViolation(
            final @NotNull UUID playerUuid,
            final @NotNull String check,
            final double violations
    ) {
        enqueue(new StorageMutation.Violation(playerUuid, check, violations, System.currentTimeMillis()), false);
    }

    public void saveInference(
            final @NotNull UUID playerUuid,
            final @NotNull StoredInference inference
    ) {
        enqueue(new StorageMutation.Inference(playerUuid, UUID.randomUUID(), inference), false);
    }

    public void saveSnapshot(final @NotNull ReactPlayer player) {
        for (final Check check : player.checkManager.checks()) {
            enqueue(new StorageMutation.Violation(
                    player.getUniqueId(),
                    check.getConfigId(),
                    check.getViolations(),
                    System.currentTimeMillis()
            ), true);
        }
    }

    public long historyCutoff() {
        return System.currentTimeMillis() - settings.historyRetention().toMillis();
    }

    public @NotNull Duration historyRetention() {
        return settings.historyRetention();
    }

    public boolean enabled() {
        return settings.enabled();
    }

    public boolean available() {
        return available.get();
    }

    public boolean closing() {
        return closing.get();
    }

    public @NotNull CompletableFuture<Void> whenAvailable() {
        return available.get() ? CompletableFuture.completedFuture(null) : availabilitySignal;
    }

    public void close(final @NotNull Collection<ReactPlayer> players) {
        if (!closing.compareAndSet(false, true)) {
            return;
        }

        if (settings.enabled()) {
            players.forEach(this::saveSnapshot);
            final CompletableFuture<Void> flush = initialization
                    .handle((ignored, failure) -> null)
                    .thenCompose(ignored -> runAsync(this::flushAll));
            try {
                flush.get(settings.shutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (final Exception ex) {
                logFailure("Could not flush all database writes before shutdown", ex);
            }
        }

        scheduler.shutdownNow();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(settings.shutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }

        backendLock.writeLock().lock();
        try {
            available.set(false);
            if (backend != null) {
                backend.close();
                backend = null;
            }
            availabilitySignal.completeExceptionally(new IllegalStateException("Database storage is closed"));
        } catch (final Exception ex) {
            logFailure("Could not close the database backend", ex);
        } finally {
            backendLock.writeLock().unlock();
        }
    }

    private void enqueue(final @NotNull StorageMutation mutation, final boolean allowClosing) {
        if (!settings.enabled() || closing.get() && !allowClosing) {
            return;
        }

        pendingWrites.addLast(mutation);
        int buffered = pendingWriteCount.incrementAndGet();
        while (buffered > settings.pendingWriteCapacity()) {
            if (pendingWrites.pollFirst() != null) {
                buffered = pendingWriteCount.decrementAndGet();
                logFailure("Database write buffer is full; the oldest unsaved operation was discarded", null);
            } else {
                break;
            }
        }
    }

    private void requestFlush() {
        if (!settings.enabled() || closing.get() || !available.get() || pendingWrites.isEmpty()) {
            return;
        }
        if (!flushRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            executor.execute(() -> {
                try {
                    flushOneBatch();
                } finally {
                    flushRunning.set(false);
                }
            });
        } catch (final RejectedExecutionException ex) {
            flushRunning.set(false);
            logFailure("Database executor queue is full; writes remain buffered", ex);
        }
    }

    private void flushOneBatch() {
        flushLock.lock();
        try {
            final List<StorageMutation> batch = drainBatch();
            if (batch.isEmpty()) {
                return;
            }

            try {
                withBackend(storage -> {
                    storage.writeBatch(batch);
                    return null;
                });
            } catch (final Exception ex) {
                requeue(batch);
                markUnavailable(ex);
            }
        } finally {
            flushLock.unlock();
        }
    }

    private void flushAll() {
        while (available.get() && !pendingWrites.isEmpty()) {
            final int before = pendingWriteCount.get();
            flushOneBatch();
            if (!available.get() || pendingWriteCount.get() >= before) {
                break;
            }
        }
    }

    private @NotNull List<StorageMutation> drainBatch() {
        final List<StorageMutation> batch = new ArrayList<>(settings.batchSize());
        while (batch.size() < settings.batchSize()) {
            final StorageMutation mutation = pendingWrites.pollFirst();
            if (mutation == null) {
                break;
            }
            pendingWriteCount.decrementAndGet();
            batch.add(mutation);
        }
        return batch;
    }

    private void requeue(final @NotNull List<StorageMutation> batch) {
        final List<StorageMutation> reversed = new ArrayList<>(batch);
        Collections.reverse(reversed);
        for (final StorageMutation mutation : reversed) {
            if (pendingWriteCount.get() >= settings.pendingWriteCapacity()) {
                break;
            }
            pendingWrites.addFirst(mutation);
            pendingWriteCount.incrementAndGet();
        }
    }

    private void requestCleanup() {
        if (!available.get() || closing.get()) {
            return;
        }
        runAsync(() -> {
            try {
                withBackend(storage -> {
                    storage.deleteHistoryBefore(historyCutoff());
                    return null;
                });
            } catch (final Exception ex) {
                markUnavailable(ex);
            }
        });
    }

    private void connect() throws Exception {
        final StorageBackend replacement = backendFactory.apply(settings);
        try {
            replacement.initialize();
            replacement.deleteHistoryBefore(historyCutoff());
        } catch (final Exception ex) {
            try {
                replacement.close();
            } catch (final Exception closeFailure) {
                ex.addSuppressed(closeFailure);
            }
            throw ex;
        }

        backendLock.writeLock().lock();
        try {
            if (closing.get()) {
                replacement.close();
                return;
            }
            final StorageBackend previous = backend;
            backend = replacement;
            synchronized (availabilityMonitor) {
                available.set(true);
                reconnectDelayMillis.set(settings.retryInitialDelay().toMillis());
                availabilitySignal.complete(null);
            }
            if (previous != null) {
                try {
                    previous.close();
                } catch (final Exception ex) {
                    logFailure("Could not close the replaced database backend", ex);
                }
            }
        } finally {
            backendLock.writeLock().unlock();
        }
    }

    private void markUnavailable(final Throwable failure) {
        if (!settings.enabled() || closing.get()) {
            return;
        }
        synchronized (availabilityMonitor) {
            available.set(false);
            if (availabilitySignal.isDone()) {
                availabilitySignal = new CompletableFuture<>();
            }
        }
        logFailure("Database operation failed; continuing in fail-open mode", failure);
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (closing.get() || !settings.enabled() || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        final long delay = reconnectDelayMillis.getAndUpdate(current -> Math.min(
                settings.retryMaximumDelay().toMillis(),
                Math.max(current + 1L, (long) (current * settings.retryMultiplier()))
        ));
        try {
            scheduler.schedule(() -> {
                reconnectScheduled.set(false);
                runAsync(this::connect).whenComplete((ignored, failure) -> {
                    if (failure == null && !closing.get()) {
                        logger.info("{} storage connection restored", settings.type());
                        requestFlush();
                    } else if (failure != null) {
                        logFailure("Could not restore the database connection", failure);
                        scheduleReconnect();
                    }
                });
            }, delay, TimeUnit.MILLISECONDS);
        } catch (final RejectedExecutionException ex) {
            reconnectScheduled.set(false);
            if (!closing.get()) {
                logFailure("Could not schedule a database reconnect", ex);
            }
        }
    }

    private <T> T withBackend(final @NotNull BackendFunction<T> action) throws Exception {
        backendLock.readLock().lock();
        try {
            if (!available.get() || backend == null) {
                throw new IllegalStateException("Database is unavailable");
            }
            return action.apply(backend);
        } finally {
            backendLock.readLock().unlock();
        }
    }

    private @NotNull CompletableFuture<Void> runAsync(final @NotNull CheckedRunnable action) {
        return supplyAsync(() -> {
            action.run();
            return null;
        });
    }

    private @NotNull CompletableFuture<Void> retryDelay() {
        final CompletableFuture<Void> delay = new CompletableFuture<>();
        try {
            scheduler.schedule(
                    () -> delay.complete(null),
                    settings.retryInitialDelay().toMillis(),
                    TimeUnit.MILLISECONDS
            );
        } catch (final RejectedExecutionException ex) {
            delay.completeExceptionally(ex);
        }
        return delay;
    }

    private boolean backendFailure(final @NotNull Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return !(cause instanceof RejectedExecutionException);
    }

    private <T> @NotNull CompletableFuture<T> supplyAsync(final @NotNull CheckedSupplier<T> supplier) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try {
                    future.complete(supplier.get());
                } catch (final Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });
        } catch (final RejectedExecutionException ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void logFailure(final @NotNull String message, final Throwable failure) {
        final long now = System.currentTimeMillis();
        final long previous = lastFailureLog.get();
        if (now - previous < FAILURE_LOG_INTERVAL_MILLIS || !lastFailureLog.compareAndSet(previous, now)) {
            return;
        }
        if (failure == null) {
            logger.warn(message);
        } else {
            logger.warn(message, failure);
        }
    }

    private static @NotNull ThreadFactory threadFactory(final @NotNull String prefix) {
        final AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            final Thread thread = new Thread(runnable, prefix + '-' + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @FunctionalInterface
    private interface BackendFunction<T> {
        T apply(@NotNull StorageBackend storage) throws Exception;
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
