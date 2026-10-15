package com.g4vrk.reactdataset.listeners.bukkit;

import com.g4vrk.reactdataset.ReactDatasetAddon;
import com.g4vrk.reactdataset.display.RecordingBossBarService;
import com.g4vrk.reactdataset.session.SessionSnapshot;
import com.g4vrk.reactdataset.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.g4vrk.react.inference.InferenceModel;

public final class RecordingConnectionListener
implements Listener {
    private final ReactDatasetAddon addon;
    private final RecordingBossBarService bossBars;

    public RecordingConnectionListener(ReactDatasetAddon addon, RecordingBossBarService bossBars) {
        this.addon = addon;
        this.bossBars = bossBars;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (this.addon.collector() == null) {
            return;
        }
        for (SessionSnapshot snapshot : this.addon.collector().active()) {
            if (snapshot.mode() == InferenceModel.FLOWER && !bossBars.restoreOnJoin()) continue;
            if (snapshot.recorderUuid() == null || !snapshot.recorderUuid().equals(event.getPlayer().getUniqueId())) continue;
            this.bossBars.show(event.getPlayer(), snapshot);
            this.addon.actions().run("recording.on-resume", (CommandSender)event.getPlayer(), Placeholders.of("player", snapshot.playerName(), "label", snapshot.label().fileLabel(), "source", snapshot.source(), "captured", snapshot.capturedFrames(), "target", snapshot.targetFrames(), "captured-frames", snapshot.capturedFrames(), "target-frames", snapshot.targetFrames(), "buffered", snapshot.buffered(), "frames-per-file", this.addon.collector().framesPerFile()));
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (addon.collector() != null) {
            SessionSnapshot snapshot = addon.collector().snapshot(event.getPlayer().getUniqueId());
            if (snapshot != null && snapshot.mode() == InferenceModel.UNIVERSE) {
                addon.collector().stop(snapshot.uniqueId());
                bossBars.hide(snapshot);
            }
        }
        this.bossBars.detach(event.getPlayer());
    }
}
