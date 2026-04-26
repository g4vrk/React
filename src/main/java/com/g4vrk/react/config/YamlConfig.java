package com.g4vrk.react.config;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;

@Getter
public class YamlConfig {

    private final Path path;
    private final YamlConfigurationLoader loader;

    private ConfigurationNode root;

    public YamlConfig(
            @NotNull Path path
    ) {
        this.path = path;
        this.loader = YamlConfigurationLoader.builder()
                .path(path)
                .build();
    }

    public void reload() {
        try {
            this.root = loader.load();
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to load config: " + path, ex);
        }
    }

    public void applyDefaults(
            final @NotNull BufferedReader reader
    ) {
        try {
            final ConfigurationNode defaults = YamlConfigurationLoader.builder()
                    .source(() -> reader)
                    .build()
                    .load();

            merge(this.root, defaults);

        } catch (final IOException ex) {
            throw new RuntimeException("Failed to apply defaults: " + path, ex);
        }
    }

    private void merge(
            final @NotNull ConfigurationNode target,
            final @NotNull ConfigurationNode defaults
    ) {
        defaults.childrenMap().forEach((key, defChild) -> {
            final ConfigurationNode targetChild = target.node(key);

            if (targetChild.virtual()) {
                try {
                    target.node(key).set(defChild.raw());
                } catch (SerializationException ignored) {
                }
            } else {
                merge(targetChild, defChild);
            }
        });
    }

    public void save() {
        try {
            loader.save(root);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save config: " + path, e);
        }
    }

    public @NotNull ConfigurationNode node(
            final @NotNull Object @NotNull ... path
    ) {
        return root.node(path);
    }
}