package com.g4vrk.reactdataset.config;

import com.g4vrk.config.Config;
import java.util.Collections;
import java.util.List;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.spongepowered.configurate.serialize.SerializationException;

public final class DatasetSettingsFactory {
    public DatasetSettings create(Config config) {
        try {
            List<String> aliases = config.node(new Object[]{"commands", "aliases"}).getList(String.class, Collections.emptyList());
            int legacyChunk = Math.max(16, config.node(new Object[]{"settings", "collector", "frames-per-file"}).getInt(150));
            int chunkFrames = Math.max(16, config.node(new Object[]{"settings", "collector", "flush-chunk-frames"}).getInt(legacyChunk));
            int legacyFiles = Math.max(1, config.node(new Object[]{"settings", "collector", "max-files-per-recording"}).getInt(10000));
            long legacyMaximum = (long)legacyFiles * (long)chunkFrames;
            int maximumFrames = Math.max(1, config.node(new Object[]{"settings", "collector", "max-recording-frames"}).getInt((int)Math.min(Integer.MAX_VALUE, legacyMaximum)));
            int defaultFrames = Math.min(maximumFrames, Math.max(1, config.node(new Object[]{"settings", "collector", "default-recording-frames"}).getInt(2000)));
            return new DatasetSettings(new DatasetSettings.CollectorSettings(config.node(new Object[]{"settings", "collector", "output-directory"}).getString("React/datasets"), config.node(new Object[]{"settings", "collector", "combat-only"}).getBoolean(true), chunkFrames, maximumFrames, defaultFrames), new DatasetSettings.UploadSettings(config.node(new Object[]{"settings", "upload", "enabled"}).getBoolean(true), config.node(new Object[]{"settings", "upload", "base-url"}).getString("http://127.0.0.1:8000"), config.node(new Object[]{"settings", "upload", "token"}).getString(""), DatasetSettings.AfterUpload.parse(config.node(new Object[]{"settings", "upload", "after-upload"}).getString("move")), Math.max(10L, config.node(new Object[]{"settings", "upload", "retry-seconds"}).getLong(120L)), Math.max(5L, config.node(new Object[]{"settings", "upload", "timeout-seconds"}).getLong(30L))), new DatasetSettings.CommandSettings(config.node(new Object[]{"commands", "literal"}).getString("collector"), List.copyOf(aliases), new DatasetSettings.Permissions(config.node(new Object[]{"commands", "permissions", "use"}).getString("react.collector"), config.node(new Object[]{"commands", "permissions", "record-start"}).getString("react.collector.record.start"), config.node(new Object[]{"commands", "permissions", "record-stop"}).getString("react.collector.record.stop"), config.node(new Object[]{"commands", "permissions", "status"}).getString("react.collector.status"), config.node(new Object[]{"commands", "permissions", "upload"}).getString("react.collector.upload"), config.node(new Object[]{"commands", "permissions", "reload"}).getString("react.collector.reload"))), new DatasetSettings.BossBarSettings(config.node(new Object[]{"settings", "bossbar", "enabled"}).getBoolean(true), DatasetSettingsFactory.parseColor(config.node(new Object[]{"settings", "bossbar", "color"}).getString("YELLOW")), DatasetSettingsFactory.parseStyle(config.node(new Object[]{"settings", "bossbar", "style"}).getString("SOLID")), config.node(new Object[]{"settings", "bossbar", "title"}).getString("&6{player} &8| &f{captured-frames}/{target-frames} \u043a\u0430\u0434\u0440\u043e\u0432 &8| &6{source}"), config.node(new Object[]{"settings", "bossbar", "restore-on-join"}).getBoolean(true)));
        }
        catch (SerializationException ex) {
            throw new IllegalStateException("Failed to read main-config.yml", ex);
        }
    }

    private static BarColor parseColor(String raw) {
        try {
            return BarColor.valueOf((String)(raw == null ? "YELLOW" : raw.trim().toUpperCase()));
        }
        catch (IllegalArgumentException ignored) {
            return BarColor.YELLOW;
        }
    }

    private static BarStyle parseStyle(String raw) {
        try {
            return BarStyle.valueOf((String)(raw == null ? "SOLID" : raw.trim().toUpperCase()));
        }
        catch (IllegalArgumentException ignored) {
            return BarStyle.SOLID;
        }
    }
}
