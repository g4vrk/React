package com.g4vrk.react.command.builder;

import com.g4vrk.react.api.Factory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class CommandBuilderFactory implements Factory<Command.Builder<CommandSender>> {

    @Getter
    private final CommandManager<CommandSender> manager;

    private final String label;
    private final String description;
    private final String[] aliases;

    public @NotNull Command.Builder<CommandSender> create() {
        return this.manager.commandBuilder(
                this.label,
                Description.of(this.description),
                this.aliases
        );
    }

}
