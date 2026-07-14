package com.g4vrk.react.resource.impl;

import com.g4vrk.react.resource.ResourceHolder;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

@RequiredArgsConstructor
public class PluginResourceHolder implements ResourceHolder {

    private final Plugin plugin;

    @Override
    public @Nullable InputStream getResource(@NotNull String fileName) {
        return this.plugin.getResource(fileName);
    }

    @Override
    public void saveResource(@NotNull String fileName, boolean replace) {
        this.plugin.saveResource(fileName, replace);
    }

}
