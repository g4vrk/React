package com.g4vrk.react.command.argument.impl;

import com.g4vrk.fastTextFormatter.placeholder.PlaceholderMap;
import com.g4vrk.react.alert.manager.AlertManager;
import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.react.command.builder.CommandBuilderFactory;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.LiteralParser;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public final class AlertsArgument extends LocalArgument {

    private static final UUID CONSOLE_UUID = UUID.fromString("0000-0000-0000-0000");

    private final CommandBuilderFactory builderFactory;

    private final AlertManager alertManager;

    @Override
    public @NotNull Command.Builder<CommandSender> build() {
        return Objects.requireNonNull(builderFactory)
                .create()
                .required("alerts", LiteralParser.literal("alerts", "notifications"))
                .handler(context -> {

                    final UUID uuid = this.uniqueId(context.sender());

                    if (!this.alertManager.remove(uuid)) {
                        this.alertManager.add(uuid);
                    }
                });
    }

    private @NotNull UUID uniqueId(
            final @NotNull CommandSender target
    ) {
        return target instanceof Player player ? player.getUniqueId() : CONSOLE_UUID;
    }

}
