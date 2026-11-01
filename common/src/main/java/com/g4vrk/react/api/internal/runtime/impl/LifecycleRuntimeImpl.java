package com.g4vrk.react.api.internal.runtime.impl;

import com.g4vrk.react.api.internal.runtime.LifecycleRuntime;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class LifecycleRuntimeImpl<T> implements LifecycleRuntime<T> {

    @FunctionalInterface
    public interface Action<T> {

        void execute(@NotNull T component) throws Exception;

    }

    private final Object lock = new Object();

    private final ObjectArrayList<T> components = new ObjectArrayList<>();

    private final ObjectArrayList<T> enabledComponents = new ObjectArrayList<>();

    private final Action<? super T> enableAction;
    private final Action<? super T> disableAction;
    private final Action<? super T> closeAction;

    private final AtomicReference<CompletableFuture<Void>> startup = new AtomicReference<>();

    private final AtomicReference<CompletableFuture<Void>> shutdown = new AtomicReference<>();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final AtomicReference<Thread> lifecycleThread = new AtomicReference<>();

    private final ExecutorService executor;

    public LifecycleRuntimeImpl(
            final @NotNull String threadName,
            final @NotNull Action<? super T> enableAction,
            final @NotNull Action<? super T> disableAction,
            final @NotNull Action<? super T> closeAction
    ) {
        this.enableAction = Objects.requireNonNull(enableAction, "enableAction");
        this.disableAction = Objects.requireNonNull(disableAction, "disableAction");
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction");

        this.executor = Executors.newSingleThreadExecutor(task -> {
            final Thread thread = new Thread(task, threadName);

            thread.setDaemon(true);
            lifecycleThread.set(thread);

            return thread;
        });
    }

    public void register(
            final @NotNull T component
    ) {
        synchronized (lock) {
            if (closed.get() || startup.get() != null) {
                throw new IllegalStateException(
                        "Cannot register components after startup"
                );
            }

            for (final T registered : components) {
                if (registered == component) {
                    throw new IllegalArgumentException(
                            "Component is already registered"
                    );
                }
            }

            components.add(component);
        }
    }

    public @NotNull CompletionStage<Void> start() {
        synchronized (lock) {
            if (closed.get()) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Runtime is closed")
                );
            }

            final CompletableFuture<Void> existing = startup.get();

            if (existing != null) {
                return existing.minimalCompletionStage();
            }

            final CompletableFuture<Void> future = new CompletableFuture<>();

            startup.set(future);

            final ObjectArrayList<T> snapshot = new ObjectArrayList<>(components);

            try {
                executor.execute(() -> enableComponents(snapshot, future));
            } catch (final RuntimeException ex) {
                closed.set(true);
                future.completeExceptionally(ex);
            }

            return future.minimalCompletionStage();
        }
    }

    public boolean running() {
        final CompletableFuture<Void> future = startup.get();

        return !closed.get()
                && future != null
                && future.isDone()
                && !future.isCompletedExceptionally()
                && !future.isCancelled();
    }

    private void enableComponents(
            final @NotNull ObjectArrayList<T> snapshot,
            final @NotNull CompletableFuture<Void> future
    ) {
        try {
            for (final T component : snapshot) {
                if (closed.get()) {
                    throw new CancellationException("Runtime is closing");
                }

                try {
                    enableAction.execute(component);
                } catch (final Throwable failure) {
                    disableFailedComponent(component, failure);

                    throw new IllegalStateException(
                            "Failed to enable component " + component.getClass().getName(),
                            failure
                    );
                }

                if (closed.get()) {
                    final CancellationException failure =
                            new CancellationException("Runtime is closing");

                    disableFailedComponent(component, failure);

                    throw failure;
                }

                enabledComponents.add(component);
            }

            if (closed.get()) {
                throw new CancellationException("Runtime is closing");
            }

            future.complete(null);

        } catch (final Throwable failure) {
            closed.set(true);

            final RuntimeException cleanupFailure = cleanup();

            if (cleanupFailure != null) {
                failure.addSuppressed(cleanupFailure);
            }

            future.completeExceptionally(failure);
        }
    }

    private void disableFailedComponent(
            final @NotNull T component,
            final @NotNull Throwable failure
    ) {
        try {
            disableAction.execute(component);
        } catch (final Throwable cleanupFailure) {
            if (cleanupFailure != failure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
    }

    private @Nullable RuntimeException disableEnabledComponents() {
        RuntimeException failure = null;

        for (int i = enabledComponents.size() - 1; i >= 0; i--) {
            final T component = enabledComponents.get(i);

            try {
                disableAction.execute(component);
            } catch (final Throwable ex) {
                if (failure == null) {
                    failure = new IllegalStateException(
                            "Failed to disable lifecycle components"
                    );
                }

                failure.addSuppressed(
                        new IllegalStateException(
                                "Failed to disable component " + component.getClass().getName(),
                                ex
                        )
                );
            }
        }

        enabledComponents.clear();

        return failure;
    }

    private @Nullable RuntimeException cleanup() {
        RuntimeException failure = disableEnabledComponents();

        for (int i = components.size() - 1; i >= 0; i--) {
            final T component = components.get(i);

            try {
                closeAction.execute(component);
            } catch (final Throwable ex) {
                if (failure == null) {
                    failure = new IllegalStateException(
                            "Failed to close lifecycle components"
                    );
                }

                failure.addSuppressed(
                        new IllegalStateException(
                                "Failed to close component " + component.getClass().getName(),
                                ex
                        )
                );
            }
        }

        components.clear();

        return failure;
    }

    @Override
    public void close() {
        final CompletableFuture<Void> pending;

        synchronized (lock) {
            closed.set(true);

            CompletableFuture<Void> future = shutdown.get();

            if (future == null) {
                future = new CompletableFuture<>();
                shutdown.set(future);

                final CompletableFuture<Void> closeFuture = future;

                try {
                    executor.execute(() -> {
                        final RuntimeException failure = cleanup();

                        if (failure == null) {
                            closeFuture.complete(null);
                        } else {
                            closeFuture.completeExceptionally(failure);
                        }
                    });
                } catch (final RuntimeException ex) {
                    closeFuture.completeExceptionally(ex);
                }
            }

            pending = future;
        }

        if (Thread.currentThread() == lifecycleThread.get()) {
            executor.shutdown();
            return;
        }

        try {
            pending.join();
        } catch (final CompletionException ex) {
            throw new IllegalStateException(
                    "Failed to close runtime",
                    ex.getCause()
            );
        } finally {
            executor.shutdown();
        }
    }
}
