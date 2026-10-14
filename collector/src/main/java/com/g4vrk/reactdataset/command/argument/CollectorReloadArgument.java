package com.g4vrk.reactdataset.command.argument;

import com.g4vrk.react.api.Factory;
import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.reactdataset.ReactDatasetAddon;
import com.g4vrk.reactdataset.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.jetbrains.annotations.NotNull;

public final class CollectorReloadArgument
extends LocalArgument {
    private final Factory<Command.Builder<CommandSender>> builderFactory;
    private final ReactDatasetAddon addon;

    public CollectorReloadArgument(Factory<Command.Builder<CommandSender>> builderFactory, ReactDatasetAddon addon) {
        this.builderFactory = builderFactory;
        this.addon = addon;
    }

    @NotNull
    public Command.Builder<CommandSender> build() {
        return this.builderFactory.create().literal("reload", new String[0]).permission(this.addon.settings().command().permissions().reload()).handler(context -> {
            CommandSender sender = (CommandSender)context.sender();
            this.addon.actions().run("reload.on-start", sender);
            try {
                this.addon.reloadAddon();
                this.addon.actions().run("reload.on-finish", sender);
            }
            catch (Throwable throwable) {
                this.addon.logger().error("Failed to reload ReactDatasetCollector", throwable);
                this.addon.actions().run("reload.on-error", sender, Placeholders.of("error", throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage()));
            }
        });
    }
}
