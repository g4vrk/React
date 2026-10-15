package com.g4vrk.reactdataset.display;

import com.g4vrk.reactdataset.config.DatasetSettings;
import com.g4vrk.reactdataset.session.SessionSnapshot;
import com.g4vrk.schedula.task.TickSchedule;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import com.g4vrk.react.inference.InferenceModel;
import com.g4vrk.reactdataset.session.RecordingSession;

public final class RecordingBossBarService {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final Scheduler scheduler;
    private final DatasetSettings.BossBarSettings settings;
    private final int framesPerChunk;
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<UUID, BossBar>();
    private final Map<UUID, UniverseBar> universeBars = new ConcurrentHashMap<>();

    public RecordingBossBarService(Scheduler scheduler, DatasetSettings.BossBarSettings settings, int framesPerChunk) {
        this.scheduler = scheduler;
        this.settings = settings;
        this.framesPerChunk = framesPerChunk;
    }

    public void show(Player player, SessionSnapshot snapshot) {
        if (snapshot != null && snapshot.mode() == InferenceModel.UNIVERSE) {
            if (player != null && player.getUniqueId().equals(snapshot.recorderUuid())) showUniverse(player, snapshot);
            return;
        }
        if (!this.settings.enabled() || player == null || snapshot == null) {
            return;
        }
        this.runSync(() -> {
            BossBar bar = this.bars.computeIfAbsent(snapshot.uniqueId(), ignored -> Bukkit.createBossBar((String)this.formatTitle(snapshot), (BarColor)this.settings.color(), (BarStyle)this.settings.style(), (BarFlag[])new BarFlag[0]));
            bar.setColor(this.settings.color());
            bar.setStyle(this.settings.style());
            bar.setTitle(this.formatTitle(snapshot));
            bar.setProgress(this.progress(snapshot));
            bar.setVisible(true);
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        });
    }

    public void update(SessionSnapshot snapshot) {
        if (snapshot != null && snapshot.mode() == InferenceModel.UNIVERSE) {
            UniverseBar entry = universeBars.get(snapshot.uniqueId());
            if (entry == null || !java.util.Objects.equals(entry.latest.recordingId(), snapshot.recordingId())) return;
            synchronized (entry) {
                if (snapshot.capturedFrames() < entry.latest.capturedFrames()
                        || snapshot.savedFrames() < entry.latest.savedFrames()) return;
                entry.latest = snapshot;
            }
            runOnViewer(entry.viewer, () -> paintUniverse(snapshot.uniqueId(), entry));
            return;
        }
        if (!this.settings.enabled() || snapshot == null) {
            return;
        }
        if (snapshot.capturedFrames() >= snapshot.targetFrames()) {
            this.hide(snapshot.uniqueId());
            return;
        }
        this.runSync(() -> {
            BossBar bar = this.bars.get(snapshot.uniqueId());
            if (bar == null) {
                return;
            }
            bar.setTitle(this.formatTitle(snapshot));
            bar.setProgress(this.progress(snapshot));
        });
    }

    public void detach(Player player) {
        if (player != null) {
            universeBars.forEach((uuid, entry) -> {
                if (entry.viewer.getUniqueId().equals(player.getUniqueId())) removeUniverse(uuid, entry);
            });
            this.runSync(() -> this.bars.values().forEach(bar -> bar.removePlayer(player)));
        }
    }

    public void hide(UUID uuid) {
        if (uuid == null) {
            return;
        }
        UniverseBar entry = universeBars.get(uuid);
        if (entry != null) removeUniverse(uuid, entry);
        this.runSync(() -> {
            BossBar bar = this.bars.remove(uuid);
            if (bar != null) {
                bar.removeAll();
                bar.setVisible(false);
            }
        });
    }

    public void clear() {
        universeBars.forEach(this::removeUniverse);
        this.runSync(() -> {
            this.bars.values().forEach(bar -> {
                bar.removeAll();
                bar.setVisible(false);
            });
            this.bars.clear();
        });
    }

    public boolean restoreOnJoin() {
        return this.settings.restoreOnJoin();
    }

    public void hide(SessionSnapshot snapshot) {
        if (snapshot.mode() != InferenceModel.UNIVERSE) {
            hide(snapshot.uniqueId());
            return;
        }
        UniverseBar entry = universeBars.get(snapshot.uniqueId());
        if (entry != null && java.util.Objects.equals(entry.latest.recordingId(), snapshot.recordingId())) {
            removeUniverse(snapshot.uniqueId(), entry);
        }
    }

    private void showUniverse(Player viewer, SessionSnapshot snapshot) {
        UniverseBar entry = new UniverseBar(viewer, snapshot);
        UniverseBar previous = universeBars.put(snapshot.uniqueId(), entry);
        if (previous != null) {
            if (java.util.Objects.equals(previous.latest.recordingId(), snapshot.recordingId())) {
                universeBars.replace(snapshot.uniqueId(), entry, previous);
                update(snapshot);
                return;
            }
            disposeUniverse(previous);
        }
        runOnViewer(viewer, () -> paintUniverse(snapshot.uniqueId(), entry));
    }

    private void paintUniverse(UUID uuid, UniverseBar entry) {
        if (universeBars.get(uuid) != entry) return;
        SessionSnapshot snapshot = entry.latest;
        String title = "UNIVERSE | " + snapshot.savedBatches() + "/" + snapshot.targetBatches()
                + " пачек | Буфер: " + snapshot.buffered() + "/64 | Кадров: "
                + snapshot.savedFrames() + " | " + snapshot.playerName();
        if (entry.bar == null) {
            entry.bar = Bukkit.createBossBar(title, BarColor.PURPLE, BarStyle.SOLID);
            entry.bar.addPlayer(entry.viewer);
            entry.bar.setVisible(true);
        }
        entry.bar.setTitle(title);
        entry.bar.setProgress(Math.max(0.0, Math.min(1.0,
                (double)snapshot.buffered() / RecordingSession.UNIVERSE_BATCH_SIZE)));
    }

    private void removeUniverse(UUID uuid, UniverseBar entry) {
        if (universeBars.remove(uuid, entry)) disposeUniverse(entry);
    }

    private void disposeUniverse(UniverseBar entry) {
        runOnViewer(entry.viewer, () -> {
            if (entry.bar != null) {
                entry.bar.removeAll();
                entry.bar.setVisible(false);
            }
        });
    }

    private void runOnViewer(Player viewer, Runnable task) {
        boolean owned = Bukkit.isPrimaryThread();
        if (!owned) {
            try {
                owned = (boolean)Bukkit.class.getMethod("isOwnedByCurrentRegion", org.bukkit.entity.Entity.class)
                        .invoke(null, viewer);
            } catch (ReflectiveOperationException unavailable) {
                // Older Paper uses the main-thread path above.
            }
        }
        if (owned) task.run();
        else scheduler.scheduleEntity(viewer, task, TickSchedule.instant());
    }

    private static final class UniverseBar {
        final Player viewer;
        volatile SessionSnapshot latest;
        BossBar bar;

        UniverseBar(Player viewer, SessionSnapshot latest) {
            this.viewer = viewer;
            this.latest = latest;
        }
    }

    private double progress(SessionSnapshot snapshot) {
        if (snapshot.targetFrames() <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double)snapshot.capturedFrames() / (double)snapshot.targetFrames()));
    }

    private String formatTitle(SessionSnapshot snapshot) {
        int percent = snapshot.targetFrames() <= 0 ? 0 : (int)Math.floor((double)snapshot.capturedFrames() * 100.0 / (double)snapshot.targetFrames());
        String title = this.settings.title().replace("{player}", snapshot.playerName()).replace("{label}", snapshot.label().fileLabel()).replace("{source}", snapshot.source()).replace("{captured}", Integer.toString(snapshot.capturedFrames())).replace("{target}", Integer.toString(snapshot.targetFrames())).replace("{captured-frames}", Integer.toString(snapshot.capturedFrames())).replace("{target-frames}", Integer.toString(snapshot.targetFrames())).replace("{buffered}", Integer.toString(snapshot.buffered())).replace("{frames-per-file}", Integer.toString(this.framesPerChunk)).replace("{flush-chunk-frames}", Integer.toString(this.framesPerChunk)).replace("{total-frames}", Integer.toString(snapshot.capturedFrames())).replace("{progress-percent}", Integer.toString(percent));
        return ChatColor.translateAlternateColorCodes((char)'&', (String)RecordingBossBarService.translateHex(title));
    }

    private static String translateHex(String input) {
        Matcher matcher = HEX.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            StringBuilder legacy = new StringBuilder("\u00a7x");
            for (char character : matcher.group(1).toCharArray()) {
                legacy.append('\u00a7').append(character);
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(legacy.toString()));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            this.scheduler.schedule(task, TickSchedule.instant());
        }
    }
}
