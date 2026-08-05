package com.g4vrk.react.api.addon.repository.impl;

import com.g4vrk.react.api.addon.repository.AddonRepository;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

public class JarAddonRepository implements AddonRepository {

    private static final String JAR_FILE_FORMAT = ".jar";

    private final File directory;

    public JarAddonRepository(
            @NotNull File directory
    ) {
        this.directory = directory;
    }

    @Override
    public @NotNull Collection<Path> discover() {

        final File[] files = directory.listFiles(
                (dir, name) -> name.endsWith(JAR_FILE_FORMAT)
        );

        if (files == null || files.length == 0) {
            return ObjectLists.emptyList();
        }

        final ObjectArrayList<Path> paths = new ObjectArrayList<>(files.length);

        for (final File file : files) {
            paths.add(file.toPath());
        }

        return paths;
    }
}
