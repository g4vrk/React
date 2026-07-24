package com.g4vrk.react.config.manager;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.functionalConfiguration.NamedConfigEntry;
import com.g4vrk.functionalConfiguration.YamlConfig;
import com.g4vrk.functionalConfiguration.loader.ConfigLoader;
import com.g4vrk.functionalConfiguration.loader.mapped.MappedConfigLoader;
import com.g4vrk.functionalConfiguration.loader.mapped.SimpleMappedConfigLoader;
import com.g4vrk.react.resource.ResourceHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Collection;
import java.util.Map;

public final class YamlConfigManager {

    private static final char JAR_PATH_SEPARATOR = '/';
    private static final String YML_FILE_FORMAT = ".yml";

    private final MappedConfigLoader<YamlConfig> mappedConfigLoader;

    private final Collection<String> expectedConfigs = new ObjectOpenHashSet<>();

    public YamlConfigManager(
            @NotNull ConfigLoader<YamlConfig> configLoader
    ) {
        this.mappedConfigLoader = new SimpleMappedConfigLoader<>(
                configLoader,
                file -> file.getParentFile().getName() + JAR_PATH_SEPARATOR + file.getName()
        );
    }

    public void expectedConfigs(
            final @NotNull String @NotNull ... names
    ) {
        this.expectedConfigs.addAll(new ObjectOpenHashSet<>(names));
    }

    public void prepareExpected(
            final @NotNull ResourceHolder resourceHolder,
            final @NotNull String languageName,
            final @NotNull File dir
    ) throws IOException {

        for (final String expected : expectedConfigs) {

            final File target = new File(dir.getParentFile(), expected);

            if (target.exists()) {
                continue;
            }

            try (final InputStream in = resourceHolder.getResource(languageName + JAR_PATH_SEPARATOR + expected)) {

                if (in == null) {
                    throw new FileNotFoundException(
                            "Resource not found: " + languageName + JAR_PATH_SEPARATOR + expected
                    );
                }

                target.getParentFile().mkdirs();

                try (final OutputStream out = new FileOutputStream(target)) {
                    in.transferTo(out);
                }
            }
        }
    }

    public @NotNull Map<String, Config> loadAndSave(
            final @NotNull File dir
    ) throws IOException {

        final Collection<File> configFiles = new ObjectOpenHashSet<>();

        collectConfigs(dir.getParentFile(), configFiles);

        return loadAndSave(configFiles);

    }

    public @NotNull Map<String, Config> loadAndSave(
            final @NotNull Iterable<File> configFiles
    ) throws IOException {

        final Map<String, Config> mapped = new Object2ObjectOpenHashMap<>();

        for (final NamedConfigEntry<YamlConfig> entry
                : mappedConfigLoader.from(configFiles)) {

            final String name = entry.name();
            final YamlConfig config = entry.config();

            config.load();

            mapped.put(
                    name.substring(name.lastIndexOf(JAR_PATH_SEPARATOR) + 1),
                    config
            );

        }

        return mapped;
    }

    private void collectConfigs(
            final @NotNull File dir,
            final @NotNull Collection<File> files
    ) {

        final File[] children = dir.listFiles();

        if (children == null) {
            return;
        }

        for (final File child : children) {

            if (child.isDirectory())
                collectConfigs(child, files);

            else if (child.getName().endsWith(YML_FILE_FORMAT))
                files.add(child);

        }

    }

}