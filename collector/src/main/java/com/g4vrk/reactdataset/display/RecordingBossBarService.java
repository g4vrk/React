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

public final class RecordingBossBarService {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final Scheduler scheduler;
    private final DatasetSettings.BossBarSettings settings;
    private final int framesPerChunk;
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<UUID, BossBar>();

    public RecordingBossBarService(Scheduler scheduler, DatasetSettings.BossBarSettings settings, int framesPerChunk) {
        this.scheduler = scheduler;
        this.settings = settings;
        this.framesPerChunk = framesPerChunk;
    }

    public void show(Player player, SessionSnapshot snapshot) {
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
            this.runSync(() -> this.bars.values().forEach(bar -> bar.removePlayer(player)));
        }
    }

    public void hide(UUID uuid) {
        if (uuid == null) {
            return;
        }
        this.runSync(() -> {
            BossBar bar = this.bars.remove(uuid);
            if (bar != null) {
                bar.removeAll();
                bar.setVisible(false);
            }
        });
    }

    public void clear() {
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
