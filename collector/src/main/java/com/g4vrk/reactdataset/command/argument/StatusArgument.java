package com.g4vrk.reactdataset.command.argument;

import com.g4vrk.react.api.Factory;
import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.reactdataset.ReactDatasetAddon;
import com.g4vrk.reactdataset.session.SessionSnapshot;
import com.g4vrk.reactdataset.upload.UploadQueue;
import com.g4vrk.reactdataset.util.Placeholders;
import java.util.Collection;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.jetbrains.annotations.NotNull;

public final class StatusArgument
extends LocalArgument {
    private final Factory<Command.Builder<CommandSender>> builderFactory;
    private final ReactDatasetAddon addon;
    private final boolean underRecord;

    public StatusArgument(Factory<Command.Builder<CommandSender>> builderFactory, ReactDatasetAddon addon, boolean underRecord) {
        this.builderFactory = builderFactory;
        this.addon = addon;
        this.underRecord = underRecord;
    }

    @NotNull
    public Command.Builder<CommandSender> build() {
        Command.Builder<CommandSender> builder = this.builderFactory.create();
        if (this.underRecord) {
            builder = builder.literal("record", new String[0]);
        }
        return builder.literal("status", new String[0]).permission(this.addon.settings().command().permissions().status()).handler(context -> this.execute((CommandSender)context.sender()));
    }

    private void execute(CommandSender sender) {
        Collection<SessionSnapshot> active = this.addon.collector().active();
        this.addon.actions().run("status.header", sender, Placeholders.of("active", active.size()));
        for (SessionSnapshot snapshot : active) {
            this.addon.actions().run("status.entry", sender, Placeholders.of("player", snapshot.playerName(), "label", snapshot.label().fileLabel(), "source", snapshot.source(), "captured", snapshot.capturedFrames(), "target", snapshot.targetFrames(), "captured-frames", snapshot.capturedFrames(), "target-frames", snapshot.targetFrames(), "buffered", snapshot.buffered()));
        }
        this.addon.actions().run("status.directory", sender, Placeholders.of("directory", this.addon.collector().datasetsDirectory(), "frames-per-file", this.addon.collector().framesPerFile(), "flush-chunk-frames", this.addon.collector().framesPerFile(), "combat-only", this.addon.collector().combatOnly()));
        UploadQueue queue = this.addon.uploadQueue();
        this.addon.actions().run("status.upload", sender, Placeholders.of("enabled", queue != null, "pending", queue == null ? 0 : queue.pendingCount(), "target", queue == null ? "disabled" : queue.target()));
    }
}
