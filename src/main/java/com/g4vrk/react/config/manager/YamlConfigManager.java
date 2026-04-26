package com.g4vrk.react.config.manager;

import com.g4vrk.react.config.YamlConfig;
import com.g4vrk.react.config.lang.Language;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class YamlConfigManager {

    private static final String YML_FILE_FORMAT = ".yml";

    private final Plugin plugin;
    private final Map<String, YamlConfig> configs = new ConcurrentHashMap<>();

    private final Language language;

    public YamlConfigManager(
            @NotNull Plugin plugin,
            @NotNull Language language
    ) {
        this.plugin = plugin;
        this.language = language;
    }

    public @NotNull YamlConfig getConfig(final @NotNull String name) {
        return configs.computeIfAbsent(name, this::createAndLoad);
    }

    public void reloadAll() {
        configs.values().forEach(YamlConfig::reload);
    }

    private @NotNull YamlConfig createAndLoad(final @NotNull String name) {
        final File file = new File(plugin.getDataFolder(), name + YML_FILE_FORMAT);

        //noinspection ResultOfMethodCallIgnored
        plugin.getDataFolder().mkdirs();

        final String langPath = name + "/" + language.name().toLowerCase() + YML_FILE_FORMAT;
        final String fallbackPath = name + "/ru" + YML_FILE_FORMAT;

        String usedPath;

        InputStream resource = plugin.getResource(langPath);

        if (resource != null) {
            usedPath = langPath;
        } else {
            resource = plugin.getResource(fallbackPath);
            usedPath = fallbackPath;
        }

        if (resource == null) {
            throw new IllegalStateException("Missing resource: " + name);
        }

        if (!file.exists()) copy(resource, file);

        final YamlConfig config = new YamlConfig(file.toPath());
        config.reload();

        final InputStream defaultsStream = plugin.getResource(usedPath);

        if (defaultsStream != null) {
            config.applyDefaults(new BufferedReader(
                    new InputStreamReader(defaultsStream, StandardCharsets.UTF_8)
            ));
            config.save();
        }

        return config;
    }

    private void copy(final @NotNull InputStream in, final @NotNull File file) {
        try (in; OutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];

            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy config", e);
        }
    }
}