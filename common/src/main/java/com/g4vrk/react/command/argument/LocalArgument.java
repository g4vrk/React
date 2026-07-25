package com.g4vrk.react.command.argument;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public abstract class LocalArgument {

    public abstract @NotNull Command.Builder<CommandSender> build();

}
