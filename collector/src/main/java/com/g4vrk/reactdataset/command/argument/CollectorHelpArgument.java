package com.g4vrk.reactdataset.command.argument;

import com.g4vrk.react.api.Factory;
import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.reactdataset.ReactDatasetAddon;
import com.g4vrk.reactdataset.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.jetbrains.annotations.NotNull;

public final class CollectorHelpArgument
extends LocalArgument {
    private final Factory<Command.Builder<CommandSender>> builderFactory;
    private final ReactDatasetAddon addon;

    public CollectorHelpArgument(Factory<Command.Builder<CommandSender>> builderFactory, ReactDatasetAddon addon) {
        this.builderFactory = builderFactory;
        this.addon = addon;
    }

    @NotNull
    public Command.Builder<CommandSender> build() {
        return this.builderFactory.create().permission(this.addon.settings().command().permissions().use()).handler(context -> this.addon.actions().run("help.on-show", (CommandSender)context.sender(), Placeholders.of("command", "/react " + this.addon.settings().command().literal(), "aliases", String.join((CharSequence)", ", this.addon.settings().command().aliases()))));
    }
}
