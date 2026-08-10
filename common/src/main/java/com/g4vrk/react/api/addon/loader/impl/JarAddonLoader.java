package com.g4vrk.react.api.addon.loader.impl;

import com.g4vrk.react.api.addon.internal.JavaAddonClassLoader;
import com.g4vrk.react.api.addon.JavaAddon;
import com.g4vrk.react.api.addon.descriptor.AddonDescriptor;
import com.g4vrk.react.api.addon.loader.AddonLoader;
import com.g4vrk.react.api.addon.meta.AddonMetadata;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.function.Function;

public class JarAddonLoader implements AddonLoader<Path> {

    private final File directory;

    private final AddonDescriptor<Path> descriptor;

    private final Function<AddonMetadata, Logger> loggerFactory;
    private final String addonMetadataFile;

    public JarAddonLoader(
            @NotNull File directory,
            @NotNull AddonDescriptor<Path> descriptor,
            @NotNull Function<AddonMetadata, Logger> loggerFactory,
            @NotNull String addonMetadataFile
    ) {
        this.directory = directory;
        this.descriptor = descriptor;
        this.loggerFactory = loggerFactory;
        this.addonMetadataFile = addonMetadataFile;
    }

    @Override
    public @NotNull JavaAddon load(@NotNull Path source) throws Exception {

        final File sourceFile = source.toFile();

        try (final FileSystem fileSystem = FileSystems.newFileSystem(source, (ClassLoader) null)) {

            final Path descriptorPath = fileSystem.getPath(addonMetadataFile);

            final AddonMetadata metadata = this.descriptor.read(descriptorPath);

            final File addonDir = new File(this.directory, metadata.name());

            //noinspection ResultOfMethodCallIgnored
            addonDir.mkdirs();

            final JavaAddonClassLoader classLoader = new JavaAddonClassLoader(
                    this.getClass().getClassLoader(),
                    metadata.mainClass(),
                    new URL[]{sourceFile.toURI().toURL()}
            );

            final JavaAddon addon = classLoader.addon();

            addon.init(
                    sourceFile,
                    addonDir,
                    metadata,
                    loggerFactory.apply(metadata),
                    classLoader
            );

            addon.onLoad();

            return addon;
        }
    }
}
