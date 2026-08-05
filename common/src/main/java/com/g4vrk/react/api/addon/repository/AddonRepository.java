package com.g4vrk.react.api.addon.repository;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;

public interface AddonRepository {

    @NotNull Collection<Path> discover();

}
