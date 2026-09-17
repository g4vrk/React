package com.g4vrk.react.command.argument.impl;

import com.g4vrk.functionalActions.list.ExecutableActionList;
import com.g4vrk.functionalActions.parser.ActionParser;
import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.Permissions;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.api.channel.ReactChannels;
import com.g4vrk.react.api.channel.impl.ChatMessageChannel;
import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.react.command.builder.CommandBuilderFactory;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.Objects;

public final class VerboseArgument extends LocalArgument implements ReloadObserver {

    private final ChatMessageChannel verboseChannel;

    private final CommandBuilderFactory builderFactory;

    private final ActionParser<Audience> actionParser;

    private ExecutableActionList<? super Audience> enabledActions;
    private ExecutableActionList<? super Audience> disabledActions;

    public VerboseArgument(
            @NotNull CommandBuilderFactory builderFactory,
            @NotNull ActionParser<Audience> actionParser
    ) {
        this.verboseChannel = ReactChannels.VERBOSE;

        this.builderFactory = builderFactory;
        this.actionParser = actionParser;

        this.reload();
    }

    @Override
    public @NotNull Command.Builder<CommandSender> build() {
        return Objects.requireNonNull(builderFactory)
                .create()
                .literal("verbose")
                .permission(Permissions.VERBOSE)
                .handler(context -> {

                    final CommandSender sender = context.sender();

                    if (!this.verboseChannel.remove(sender)) {

                        this.verboseChannel.subscribe(sender);

                        if (this.enabledActions != null) this.enabledActions.run(sender);

                    } else {

                        if (this.disabledActions != null) this.disabledActions.run(sender);

                    }
                });
    }

    public void reload() {

        final Config actionsConfig = React.INSTANCE.getActionsConfig();

        this.onReload(actionsConfig);

    }

    @Override
    public void onReload(@NotNull Config config) {

        try {
            this.enabledActions = this.actionParser.parseAll(
                    config.node("verbose", "on-enable").getList(String.class, Collections.emptyList())
            );
            this.disabledActions = this.actionParser.parseAll(
                    config.node("verbose", "on-disable").getList(String.class, Collections.emptyList())
            );
        } catch (final SerializationException ex) {
            throw new RuntimeException("Failed to load configuration values for alerts argument", ex);
        }

    }
}
