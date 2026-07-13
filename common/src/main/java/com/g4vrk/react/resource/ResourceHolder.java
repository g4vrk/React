package com.g4vrk.react.resource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

public interface ResourceHolder {

    @Nullable InputStream getResource(@NotNull String fileName);

    void saveResource(@NotNull String fileName, boolean replace);

}
