package com.g4vrk.reactdataset.config;

import java.nio.file.Path;
import java.util.List;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

public record DatasetSettings(CollectorSettings collector, UploadSettings upload, CommandSettings command, BossBarSettings bossBar) {
    public Path resolveDatasetsDirectory(Path reactDataFolder) {
        Path configured = Path.of(this.collector.outputDirectory(), new String[0]);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return reactDataFolder.getParent().resolve(configured).normalize();
    }

    public record CollectorSettings(String outputDirectory, boolean combatOnly, int framesPerFile, int maxFilesPerRecording, int defaultRecordingFrames) {
        public int flushChunkFrames() {
            return this.framesPerFile;
        }

        public int maxRecordingFrames() {
            return this.maxFilesPerRecording;
        }
    }

    public record UploadSettings(boolean enabled, String baseUrl, String token, AfterUpload afterUpload, long retrySeconds, long timeoutSeconds) {
    }

    public record CommandSettings(String literal, List<String> aliases, Permissions permissions) {
    }

    public record BossBarSettings(boolean enabled, BarColor color, BarStyle style, String title, boolean restoreOnJoin) {
    }

    public static enum AfterUpload {
        MOVE,
        DELETE,
        KEEP;


        public static AfterUpload parse(String value) {
            if (value == null) {
                return MOVE;
            }
            try {
                return AfterUpload.valueOf(value.trim().toUpperCase());
            }
            catch (IllegalArgumentException ignored) {
                return MOVE;
            }
        }
    }

    public record Permissions(String use, String start, String stop, String status, String upload, String reload) {
    }
}
