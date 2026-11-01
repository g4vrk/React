package com.g4vrk.react.api.addon.internal.manager;

import com.g4vrk.config.loader.YamlConfigLoader;
import com.g4vrk.react.api.addon.JavaAddon;
import com.g4vrk.react.api.addon.internal.descriptor.AddonDescriptor;
import com.g4vrk.react.api.addon.internal.loader.JarAddonLoader;
import com.g4vrk.react.api.internal.manager.AbstractRuntimeManager;
import com.g4vrk.react.api.internal.runtime.LifecycleRuntime;
import com.g4vrk.react.api.internal.runtime.impl.LifecycleRuntimeImpl;
import com.g4vrk.react.api.repository.impl.JarRepository;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public final class AddonManager extends AbstractRuntimeManager<JavaAddon> {

    private static final String COMPONENT_TYPE = "addon";

    private static final String DIRECTORY_NAME = "addons";
    private static final String METADATA_FILE = "react-addon.yml";

    private static final String THREAD_NAME = "React-Addon-Runtime";
    private static final String LOGGER_SEPARATOR = "#";

    private final File directory;

    private final JarAddonLoader loader;

    public AddonManager(
            final @NotNull File pluginDirectory,
            final @NotNull Logger logger
    ) {
        super(
                COMPONENT_TYPE,
                logger,
                createRuntime(),
                directory(pluginDirectory),
                new JarRepository(directory(pluginDirectory))
        );

        this.directory = directory(pluginDirectory);

        this.loader = new JarAddonLoader(
                directory,
                new AddonDescriptor(new YamlConfigLoader()),
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

    private static @NotNull LifecycleRuntime<JavaAddon> createRuntime() {
        return new LifecycleRuntimeImpl<>(
                THREAD_NAME,
                addon -> addon.setEnabled(true),
                addon -> addon.setEnabled(false),
                JavaAddon::closeClassLoader
        );
    }

    @Override
    protected @NotNull JavaAddon loadComponent(
            final @NotNull Path source
    ) throws Exception {
        return loader.load(source);
    }

    @Override
    protected @NotNull String componentName(
            final @NotNull Path source,
            final @NotNull JavaAddon addon
    ) {
        return addon.name();
    }

    @Override
    protected @NotNull String componentKey(
            final @NotNull Path source,
            final @NotNull JavaAddon addon
    ) {
        return addon.name();
    }

    @Override
    protected void releaseUnregistered(
            final @NotNull JavaAddon addon
    ) throws Exception {
        addon.closeClassLoader();
    }

    public @NotNull File directory() {
        return directory;
    }

    public @NotNull Map<String, JavaAddon> addons() {
        final Object2ObjectOpenHashMap<String, JavaAddon> addons =
                new Object2ObjectOpenHashMap<>();

        for (final JavaAddon addon : components()) {
            addons.put(addon.name(), addon);
        }

        return Collections.unmodifiableMap(addons);
    }
}