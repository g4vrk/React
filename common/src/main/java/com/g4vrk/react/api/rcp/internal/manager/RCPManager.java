package com.g4vrk.react.api.rcp.internal.manager;

import com.g4vrk.config.loader.YamlConfigLoader;
import com.g4vrk.react.api.internal.manager.AbstractRuntimeManager;
import com.g4vrk.react.api.internal.runtime.LifecycleRuntime;
import com.g4vrk.react.api.internal.runtime.impl.LifecycleRuntimeImpl;
import com.g4vrk.react.api.rcp.RCPLifecycle;
import com.g4vrk.react.api.rcp.internal.descriptor.RCPDescriptor;
import com.g4vrk.react.api.rcp.internal.loader.JarRCPLoader;
import com.g4vrk.react.api.repository.impl.JarRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

public final class RCPManager extends AbstractRuntimeManager<RCPLifecycle> {

    private static final String COMPONENT_TYPE = "RCP";

    private static final String DIRECTORY_NAME = ".rcp";
    private static final String PROVIDERS_DIRECTORY_NAME = "providers";
    private static final String METADATA_FILE = "rcp-metadata.yml";

    private static final String THREAD_NAME = "React-RCP-Runtime";
    private static final String LOGGER_SEPARATOR = "#";

    private final File directory;
    private final File providersDirectory;

    private final JarRCPLoader loader;

    public RCPManager(
            final @NotNull File pluginDirectory,
            final @NotNull Logger logger
    ) {
        super(
                COMPONENT_TYPE,
                logger,
                createRuntime(),
                providersDirectory(pluginDirectory),
                new JarRepository(providersDirectory(pluginDirectory))
        );

        this.directory = directory(pluginDirectory);
        this.providersDirectory = providersDirectory(pluginDirectory);

        this.loader = new JarRCPLoader(
                directory,
                new RCPDescriptor(new YamlConfigLoader()),
                metadata -> LoggerFactory.getLogger(
                        logger.getName() + LOGGER_SEPARATOR + metadata.name()
                ),
                METADATA_FILE
        );
    }

    private static @NotNull File directory(
            final @NotNull File pluginDirectory
    ) {
        return new File(pluginDirectory, DIRECTORY_NAME);
    }

    private static @NotNull File providersDirectory(
            final @NotNull File pluginDirectory
    ) {
        return new File(
                directory(pluginDirectory),
                PROVIDERS_DIRECTORY_NAME
        );
    }

    private static @NotNull LifecycleRuntime<RCPLifecycle> createRuntime() {
        return new LifecycleRuntimeImpl<>(
                THREAD_NAME,
                RCPLifecycle::onEnable,
                RCPLifecycle::onDisable,
                lifecycle -> {}
        );
    }

    @Override
    protected @NotNull RCPLifecycle loadComponent(
            final @NotNull Path source
    ) throws Exception {
        return loader.load(source);
    }

    @Override
    protected @NotNull String componentName(
            final @NotNull Path source,
            final @NotNull RCPLifecycle lifecycle
    ) {
        return source.getFileName().toString();
    }

    @Override
    protected void releaseUnregistered(
            final @NotNull RCPLifecycle lifecycle
    ) {
        // classloaders are managed by loader
    }

    @Override
    protected void closeResources() throws Exception {
        loader.close();
    }

    public @NotNull File directory() {
        return directory;
    }

    public @NotNull File providersDirectory() {
        return providersDirectory;
    }
}