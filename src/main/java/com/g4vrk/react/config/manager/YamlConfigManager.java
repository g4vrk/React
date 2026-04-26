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

    public static final String YML_FILE_FORMAT = ".yml";

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
        final String fileName = name.endsWith(YML_FILE_FORMAT)
                ? name.substring(0, name.length() - YML_FILE_FORMAT.length())
                : name;

        final File file = new File(plugin.getDataFolder(), fileName + YML_FILE_FORMAT);

        //noinspection ResultOfMethodCallIgnored
        plugin.getDataFolder().mkdirs();

        final String langPath = fileName + "/" + language.name().toLowerCase() + YML_FILE_FORMAT;
        final String fallbackPath = fileName + "/ru" + YML_FILE_FORMAT;

        final String usedPath;

        InputStream resource = plugin.getResource(langPath);

        if (resource != null) {
            usedPath = langPath;
        } else {
            resource = plugin.getResource(fallbackPath);
            usedPath = fallbackPath;
        }

        if (resource == null) {
            throw new IllegalStateException("Missing resource: " + fileName + YML_FILE_FORMAT);
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

    private void copy(
            final @NotNull InputStream in,
            final @NotNull File file
    ) {
        try (in; OutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];

            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to copy config", ex);
        }
    }
}