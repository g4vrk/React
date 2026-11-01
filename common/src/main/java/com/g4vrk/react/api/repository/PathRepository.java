package com.g4vrk.react.api.repository;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;

public interface PathRepository {

    @NotNull Collection<Path> discover();

}
