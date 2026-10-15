package com.g4vrk.reactdataset.command.argument;

import com.g4vrk.react.api.Factory;
import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.reactdataset.ReactDatasetAddon;
import com.g4vrk.reactdataset.collector.DatasetCollector;
import com.g4vrk.reactdataset.model.RecordLabel;
import com.g4vrk.reactdataset.notification.RecordingNotifier;
import com.g4vrk.reactdataset.session.SessionSnapshot;
import com.g4vrk.reactdataset.util.Placeholders;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;
import com.g4vrk.react.inference.InferenceModel;
import com.g4vrk.reactdataset.session.RecordingSession;

public final class RecordStartArgument
extends LocalArgument {
    private final Factory<Command.Builder<CommandSender>> builderFactory;
    private final ReactDatasetAddon addon;
    private final RecordLabel label;

    public RecordStartArgument(Factory<Command.Builder<CommandSender>> builderFactory, ReactDatasetAddon addon, RecordLabel label) {
        this.builderFactory = builderFactory;
        this.addon = addon;
        this.label = label;
    }

    @NotNull
    public Command.Builder<CommandSender> build() {
        return build(false, false);
    }

    public Command.Builder<CommandSender> buildNew(boolean explicitFrames) {
        return build(true, explicitFrames);
    }

    private Command.Builder<CommandSender> build(boolean universe, boolean explicitFrames) {
        Command.Builder<CommandSender> builder = this.builderFactory.create().literal("record", new String[0]).literal("start", new String[0]);
        builder = this.label == RecordLabel.SOFT ? builder.literal("soft", new String[]{"cheater"}) : builder.literal("legit", new String[0]);
        builder = builder.required("player", SinglePlayerSelectorParser.singlePlayerSelectorParser()).required("source", StringParser.stringParser());
        if (universe) {
            if (explicitFrames) builder = builder.required("frames", IntegerParser.integerParser());
            builder = builder.literal("--new");
        } else {
            builder = builder.optional("frames", IntegerParser.integerParser());
        }
        return builder.permission(this.addon.settings().command().permissions().start()).handler(context -> this.execute((CommandSender)context.sender(), (SinglePlayerSelector)context.get("player"), (String)context.get("source"), (Integer)context.getOrDefault("frames", universe
                ? Math.max(1, this.addon.settings().collector().defaultRecordingFrames() / RecordingSession.UNIVERSE_BATCH_SIZE)
                : this.addon.settings().collector().defaultRecordingFrames()), universe));
    }

    private void execute(CommandSender sender, SinglePlayerSelector selector, String source, int count, boolean universe) {
        UUID uUID;
        Player player;
        final DatasetCollector collector = this.addon.collector();
        if (collector == null) {
            return;
        }
        Player player2 = player = selector == null ? null : (Player)selector.single();
        if (player == null) {
            this.addon.actions().run("recording.on-player-not-found", sender, Placeholders.of("player", "?"));
            return;
        }
        final int maximum = universe ? collector.maxFramesPerRecording() / RecordingSession.UNIVERSE_BATCH_SIZE : collector.maxFramesPerRecording();
        if (count < 1 || count > maximum) {
            this.addon.actions().run(universe ? "recording.on-invalid-batch-count" : "recording.on-invalid-count", sender,
                    Placeholders.of("count", count, "max-files", maximum, "max-frames", maximum, "max-batches", maximum));
            return;
        }
        final int frames = universe ? Math.multiplyExact(count, RecordingSession.UNIVERSE_BATCH_SIZE) : count;
        final UUID targetUuid = player.getUniqueId();
        if (sender instanceof Player) {
            Player senderPlayer = (Player)sender;
            uUID = senderPlayer.getUniqueId();
        } else {
            uUID = null;
        }
        final UUID senderUuid = uUID;
        final CommandSender fallbackSender = sender;
        final String targetName = player.getName();
        final String safeSource = source;
        final int chunkFrames = universe ? RecordingSession.UNIVERSE_BATCH_SIZE : collector.framesPerFile();
        Map<String, String> base = Placeholders.of("player", targetName, "label", this.label.fileLabel(), "source", safeSource, "frames", frames, "target-frames", frames, "batches", universe ? count : 0, "file-count", universe ? count : (frames + chunkFrames - 1) / chunkFrames, "frames-per-file", chunkFrames, "combat-only", collector.combatOnly(), "directory", collector.datasetsDirectory().toString());
        RecordingNotifier notifier = new RecordingNotifier(){

            @Override
            public void onFile(Path path, int capturedFrames, int targetFrames) {
                RecordStartArgument.this.addon.actions().run("recording.on-file", RecordStartArgument.this.resolveSender(fallbackSender, senderUuid), Placeholders.of("player", targetName, "label", RecordStartArgument.this.label.fileLabel(), "source", safeSource, "file", path.getFileName(), "captured", capturedFrames, "target", targetFrames, "captured-frames", capturedFrames, "target-frames", targetFrames, "frames-per-file", chunkFrames));
            }

            @Override
            public void onComplete(int capturedFrames, int targetFrames) {
                if (!universe) RecordStartArgument.this.addon.recordingBossBars().hide(targetUuid);
                RecordStartArgument.this.addon.actions().run("recording.on-complete", RecordStartArgument.this.resolveSender(fallbackSender, senderUuid), Placeholders.of("player", targetName, "label", RecordStartArgument.this.label.fileLabel(), "source", safeSource, "captured", capturedFrames, "target", targetFrames, "captured-frames", capturedFrames, "target-frames", targetFrames, "buffered", 0, "frames-per-file", chunkFrames));
            }

            @Override
            public void onError() {
                RecordStartArgument.this.addon.actions().run("recording.on-error", RecordStartArgument.this.resolveSender(fallbackSender, senderUuid), Placeholders.of("player", targetName, "label", RecordStartArgument.this.label.fileLabel(), "source", safeSource));
            }
        };
        if (!collector.start(player, this.label, frames, source, senderUuid, notifier, universe ? InferenceModel.UNIVERSE : InferenceModel.FLOWER)) {
            this.addon.actions().run("recording.on-already-recording", sender, Placeholders.of("player", player.getName()));
            return;
        }
        SessionSnapshot snapshot = collector.snapshot(targetUuid);
        if (snapshot != null && sender instanceof Player) {
            Player recorder = (Player)sender;
            this.addon.recordingBossBars().show(recorder, snapshot);
        }
        this.addon.actions().run("recording.on-start", sender, base);
    }

    private CommandSender resolveSender(CommandSender fallback, UUID senderUuid) {
        if (senderUuid == null) {
            return fallback;
        }
        return Bukkit.getPlayer((UUID)senderUuid);
    }
}
