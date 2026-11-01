package com.g4vrk.react.api.internal.manager;

import com.g4vrk.react.api.internal.runtime.LifecycleRuntime;
import com.g4vrk.react.api.repository.PathRepository;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractRuntimeManager<T> implements AutoCloseable {

    private final Object lock = new Object();

    private final String componentType;
    private final Logger logger;
    private final LifecycleRuntime<T> runtime;

    private final File directory;
    private final PathRepository repository;

    private final ObjectArrayList<T> components = new ObjectArrayList<>();
    private final ObjectOpenHashSet<String> componentKeys = new ObjectOpenHashSet<>();

    private final AtomicBoolean loaded = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    protected AbstractRuntimeManager(
            final @NotNull String componentType,
            final @NotNull Logger logger,
            final @NotNull LifecycleRuntime<T> runtime,
            final @NotNull File directory,
            final @NotNull PathRepository repository
    ) {
        this.componentType = Objects.requireNonNull(componentType, "componentType");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.directory = Objects.requireNonNull(directory, "directory");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    protected abstract @NotNull T loadComponent(
            @NotNull Path source
    ) throws Exception;

    protected abstract @NotNull String componentName(
            @NotNull Path source,
            @NotNull T component
    );

    protected @Nullable String componentKey(
            @NotNull Path source,
            @NotNull T component
    ) {
        return null;
    }

    protected abstract void releaseUnregistered(
            @NotNull T component
    ) throws Exception;

    protected void closeResources() throws Exception {
    }

    public final void load() {

        synchronized (lock) {
            if (closed.get()) {
                throw new IllegalStateException(componentType + " manager is closed");
            }

            if (!loaded.compareAndSet(false, true)) {
                return;
            }
        }

        try {
            Files.createDirectories(directory.toPath());
        } catch (final IOException ex) {
            throw new IllegalStateException(
                    "Cannot create " + componentType + " directory",
                    ex
            );
        }

        final Collection<Path> sources = repository.discover();

        for (final Path source : sources) {

            T component = null;

            String key = null;
            boolean keyAdded = false;
            boolean registered = false;

            try {

                component = loadComponent(source);

                key = componentKey(source, component);

                if (key != null) {

                    final String normalizedKey = key.toLowerCase(Locale.ROOT);

                    synchronized (lock) {
                        if (!componentKeys.add(normalizedKey)) {
                            throw new IllegalArgumentException(
                                    componentType + " '" + key + "' is already loaded"
                            );
                        }
                    }

                    keyAdded = true;
                }

                runtime.register(component);

                registered = true;

                synchronized (lock) {
                    components.add(component);
                }

                logger.info(
                        "Loaded {} '{}'",
                        componentType,
                        componentName(source, component)
                );

            } catch (final Exception | LinkageError ex) {

                if (keyAdded && !registered) {
                    synchronized (lock) {
                        componentKeys.remove(key.toLowerCase(Locale.ROOT));
                    }
                }

                if (component != null && !registered) {
                    try {
                        releaseUnregistered(component);
                    } catch (final Exception releaseException) {
                        ex.addSuppressed(releaseException);
                    }
                }

                logger.error(
                        "Failed to load {} from {}",
                        componentType,
                        source,
                        ex
                );
            }
        }

        logger.info(
                "Loaded {} {} components",
                components.size(),
                componentType
        );
    }

    public final @NotNull CompletionStage<Void> start() {

        load();

        return runtime.start().whenComplete((ignored, failure) -> {
            if (failure != null) {
                logger.error(
                        "Failed to start {} runtime",
                        componentType,
                        failure
                );
            }
        });
    }

    public final boolean running() {
        return runtime.running();
    }

    public final @NotNull List<T> components() {
        synchronized (lock) {
            return List.copyOf(components);
        }
    }

    @Override
    public final void close() {

        if (!closed.compareAndSet(false, true)) {
            return;
        }

        RuntimeException failure = null;

        try {
            runtime.close();
        } catch (final RuntimeException ex) {
            failure = new IllegalStateException(
                    "Failed to close " + componentType + " runtime",
                    ex
            );
        }

        try {
            closeResources();
        } catch (final Exception | LinkageError ex) {

            if (failure == null) {
                failure = new IllegalStateException(
                        "Failed to close " + componentType + " resources"
                );
            }

            failure.addSuppressed(ex);
        }

        synchronized (lock) {
            components.clear();
            componentKeys.clear();
        }

        if (failure != null) {
            throw failure;
        }
    }
}