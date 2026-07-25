package com.g4vrk.react.command.argument.impl;

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
import java.util.function.Supplier;

@RequiredArgsConstructor
public final class AlertsArgument extends LocalArgument {

    private static final UUID CONSOLE_UUID = new UUID(0, 0);

    private final CommandBuilderFactory builderFactory;

    private final Supplier<AlertManager> alertManagerSupplier;

    @Override
    public @NotNull Command.Builder<CommandSender> build() {
        return Objects.requireNonNull(builderFactory)
                .create()
                .required("alerts", LiteralParser.literal("alerts", "notifications"))
                .handler(context -> {

                    final AlertManager alertManager = this.alertManagerSupplier.get();
                    final UUID uuid = this.uniqueId(context.sender());

                    if (!alertManager.remove(uuid)) {
                        alertManager.add(uuid);
                    }
                });
    }

    private @NotNull UUID uniqueId(
            final @NotNull CommandSender target
    ) {
        return target instanceof Player player ? player.getUniqueId() : CONSOLE_UUID;
    }

}
