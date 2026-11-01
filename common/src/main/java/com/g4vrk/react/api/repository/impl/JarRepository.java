package com.g4vrk.react.api.repository.impl;

import com.g4vrk.react.api.repository.PathRepository;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

public class JarRepository implements PathRepository {

    private static final String JAR_FILE_FORMAT = ".jar";

    private final File directory;

    public JarRepository(
            final @NotNull File directory
    ) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    @Override
    public @NotNull Collection<Path> discover() {

        final File[] files = directory.listFiles(
                (dir, name) -> name.length() >= JAR_FILE_FORMAT.length()
                        && name.regionMatches(
                        true,
                        name.length() - JAR_FILE_FORMAT.length(),
                        JAR_FILE_FORMAT,
                        0,
                        JAR_FILE_FORMAT.length()
                )
        );

        if (files == null || files.length == 0) {
            return ObjectLists.emptyList();
        }

        final ObjectArrayList<Path> paths = new ObjectArrayList<>(files.length);

        for (final File file : files) {
            if (file.isFile()) {
                paths.add(file.toPath());
            }
        }

        return paths;
    }
}