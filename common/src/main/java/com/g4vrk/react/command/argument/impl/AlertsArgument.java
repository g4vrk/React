package com.g4vrk.react.command.argument.impl;

import com.g4vrk.functionalActions.list.ExecutableActionList;
import com.g4vrk.functionalActions.parser.ActionParser;
import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.alert.manager.AlertManager;
import com.g4vrk.react.alert.publish.impl.AlertPublisher;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.react.command.builder.CommandBuilderFactory;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.LiteralParser;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

public final class AlertsArgument extends LocalArgument implements ReloadObserver {

    private static final UUID CONSOLE_UUID = new UUID(0, 0);

    private final CommandBuilderFactory builderFactory;

    private final AlertPublisher alertPublisher;
    private final AlertManager alertManager;

    private final ActionParser<Audience> actionParser;

    private ExecutableActionList<? super Audience> enabledActions;
    private ExecutableActionList<? super Audience> disabledActions;

    public AlertsArgument(
            @NotNull CommandBuilderFactory builderFactory,
            @NotNull AlertPublisher alertPublisher,
            @NotNull AlertManager alertManager,
            @NotNull ActionParser<Audience> actionParser
    ) {
        this.builderFactory = builderFactory;
        this.alertPublisher = alertPublisher;
        this.alertManager = alertManager;
        this.actionParser = actionParser;

        this.reload();
    }

    @Override
    public @NotNull Command.Builder<CommandSender> build() {
        return Objects.requireNonNull(builderFactory)
                .create()
                .required("alerts", LiteralParser.literal("alerts", "notifications"))
                .handler(context -> {

                    final CommandSender sender = context.sender();
                    final UUID uuid = this.uniqueId(sender);

                    if (!this.alertManager.remove(uuid)) {

                        this.alertManager.add(uuid);

                        if (this.enabledActions != null) this.enabledActions.run(sender);

                    } else {

                        if (this.disabledActions != null) this.disabledActions.run(sender);

                    }

                    this.alertPublisher.flushAsync();
                });
    }

    private @NotNull UUID uniqueId(
            final @NotNull CommandSender target
    ) {
        return target instanceof Player player ? player.getUniqueId() : CONSOLE_UUID;
    }

    public void reload() {

        final Config actionsConfig = React.INSTANCE.getActionsConfig();

        this.onReload(actionsConfig);

    }

    @Override
    public void onReload(@NotNull Config config) {

        try {
            this.enabledActions = this.actionParser.parseAll(
                    config.node("alerts", "on-enable").getList(String.class, Collections.emptyList())
            );
            this.disabledActions = this.actionParser.parseAll(
                    config.node("alerts", "on-disable").getList(String.class, Collections.emptyList())
            );
        } catch (final SerializationException ex) {
            throw new RuntimeException("Failed to load configuration values for alerts argument", ex);
        }

    }
}
