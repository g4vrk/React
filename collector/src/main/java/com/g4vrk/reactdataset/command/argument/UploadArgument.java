package com.g4vrk.reactdataset.command.argument;

import com.g4vrk.react.api.Factory;
import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.reactdataset.ReactDatasetAddon;
import com.g4vrk.reactdataset.upload.UploadQueue;
import com.g4vrk.reactdataset.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.jetbrains.annotations.NotNull;

public final class UploadArgument
extends LocalArgument {
    private final Factory<Command.Builder<CommandSender>> builderFactory;
    private final ReactDatasetAddon addon;

    public UploadArgument(Factory<Command.Builder<CommandSender>> builderFactory, ReactDatasetAddon addon) {
        this.builderFactory = builderFactory;
        this.addon = addon;
    }

    @NotNull
    public Command.Builder<CommandSender> build() {
        return this.builderFactory.create().literal("upload", new String[0]).permission(this.addon.settings().command().permissions().upload()).handler(context -> this.execute((CommandSender)context.sender()));
    }

    private void execute(CommandSender sender) {
        UploadQueue queue = this.addon.uploadQueue();
        if (queue == null) {
            this.addon.actions().run("upload.on-disabled", sender);
            return;
        }
        int pending = queue.pendingCount();
        this.addon.collector().executeAsync(queue::processPending);
        this.addon.actions().run("upload.on-start", sender, Placeholders.of("pending", pending, "target", queue.target()));
    }
}
