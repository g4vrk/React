package com.g4vrk.react.api.rcp.internal.loader;

import com.g4vrk.react.api.Loader;
import com.g4vrk.react.api.rcp.RCPLifecycle;
import com.g4vrk.react.api.rcp.environment.RCPEnvironment;
import com.g4vrk.react.api.rcp.exception.InvalidRCPException;
import com.g4vrk.react.api.rcp.internal.JavaRCPClassLoader;
import com.g4vrk.react.api.rcp.internal.descriptor.RCPDescriptor;
import com.g4vrk.react.api.rcp.meta.RCPMetadata;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class JarRCPLoader implements Loader<RCPLifecycle, Path>, AutoCloseable {

    private final File directory;
    private final RCPDescriptor descriptor;
    private final Function<RCPMetadata, Logger> loggerFactory;
    private final String rcpMetadataFile;

    private final List<JavaRCPClassLoader> classLoaders = new ArrayList<>();

    public JarRCPLoader(
            final @NotNull File directory,
            final @NotNull RCPDescriptor descriptor,
            final @NotNull Function<RCPMetadata, Logger> loggerFactory,
            final @NotNull String rcpMetadataFile
    ) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.loggerFactory = Objects.requireNonNull(loggerFactory, "loggerFactory");
        this.rcpMetadataFile = Objects.requireNonNull(rcpMetadataFile, "rcpMetadataFile");
    }

    @Override
    public @NotNull RCPLifecycle load(@NotNull final Path source) throws Exception {

        final File sourceFile = source.toFile();

        final RCPMetadata metadata;

        try (final FileSystem fileSystem = FileSystems.newFileSystem(source, (ClassLoader) null)) {

            final Path metadataPath = fileSystem.getPath(rcpMetadataFile);

            metadata = descriptor.read(metadataPath);
        }

        final Path rootDirectory = directory.toPath().toAbsolutePath().normalize();

        Files.createDirectories(rootDirectory);

        final Path namePath = Path.of(metadata.name());

        if (namePath.isAbsolute()
                || namePath.getNameCount() != 1
                || namePath.toString().equals(".")
                || namePath.toString().equals("..")) {

            throw new InvalidRCPException(
                    "Invalid RCP name '" + metadata.name() + "'"
            );
        }

        final Path environmentDirectory = rootDirectory.resolve(namePath).normalize();

        Files.createDirectories(environmentDirectory);

        final RCPEnvironment environment = () -> environmentDirectory;

        final JavaRCPClassLoader classLoader = new JavaRCPClassLoader(
                RCPLifecycle.class.getClassLoader(),
                metadata.mainClass(),
                new URL[]{sourceFile.toURI().toURL()},
                environment
        );

        try {

            final RCPLifecycle lifecycle = classLoader.lifecycle();

            final Logger logger = Objects.requireNonNull(
                    loggerFactory.apply(metadata),
                    "logger cannot be null"
            );

            logger.info(
                    "Loaded RCP '{}' v{}",
                    metadata.name(),
                    metadata.version()
            );

            classLoaders.add(classLoader);

            return lifecycle;

        } catch (final Exception | LinkageError ex) {

            try {
                classLoader.close();
            } catch (final IOException closeException) {
                ex.addSuppressed(closeException);
            }

            throw ex;
        }
    }

    @Override
    public void close() throws IOException {

        IOException exception = null;

        for (int index = classLoaders.size() - 1; index >= 0; index--) {

            try {

                classLoaders.get(index).close();

            } catch (final IOException ex) {

                if (exception == null) {
                    exception = ex;
                } else {
                    exception.addSuppressed(ex);
                }
            }
        }

        classLoaders.clear();

        if (exception != null) {
            throw exception;
        }
    }
}