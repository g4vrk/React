package com.g4vrk.reactdataset.command.argument;

import com.g4vrk.react.api.Factory;
import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.reactdataset.ReactDatasetAddon;
import com.g4vrk.reactdataset.session.SessionSnapshot;
import com.g4vrk.reactdataset.util.Placeholders;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;
import org.jetbrains.annotations.NotNull;

public final class RecordStopArgument
extends LocalArgument {
    private final Factory<Command.Builder<CommandSender>> builderFactory;
    private final ReactDatasetAddon addon;

    public RecordStopArgument(Factory<Command.Builder<CommandSender>> builderFactory, ReactDatasetAddon addon) {
        this.builderFactory = builderFactory;
        this.addon = addon;
    }

    @NotNull
    public Command.Builder<CommandSender> build() {
        return this.builderFactory.create().literal("record", new String[0]).literal("stop", new String[0]).optional("player", SinglePlayerSelectorParser.singlePlayerSelectorParser()).permission(this.addon.settings().command().permissions().stop()).handler(context -> this.execute((CommandSender)context.sender(), (SinglePlayerSelector)context.getOrDefault("player", null)));
    }

    private void execute(CommandSender sender, SinglePlayerSelector selector) {
        Player player;
        if (selector == null) {
            if (!(sender instanceof Player)) {
                this.addon.actions().run("recording.on-player-not-found", sender, Placeholders.of("player", "<required>"));
                return;
            }
            Player self = (Player)sender;
            player = self;
        } else {
            player = (Player)selector.single();
            if (player == null) {
                this.addon.actions().run("recording.on-player-not-found", sender, Placeholders.of("player", "?"));
                return;
            }
        }
        UUID uuid = player.getUniqueId();
        SessionSnapshot snapshot = this.addon.collector().stop(uuid);
        if (snapshot == null) {
            this.addon.actions().run("recording.on-not-recording", sender, Placeholders.of("player", player.getName()));
            return;
        }
        this.addon.recordingBossBars().hide(snapshot);
        this.addon.actions().run("recording.on-stop", sender, Placeholders.of("player", snapshot.playerName(), "label", snapshot.label().fileLabel(), "source", snapshot.source(), "captured", snapshot.capturedFrames(), "target", snapshot.targetFrames(), "captured-frames", snapshot.capturedFrames(), "target-frames", snapshot.targetFrames(), "buffered", snapshot.buffered(), "frames-per-file", this.addon.collector().framesPerFile()));
    }
}
