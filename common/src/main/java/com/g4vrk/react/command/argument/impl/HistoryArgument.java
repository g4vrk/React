package com.g4vrk.react.command.argument.impl;

import com.g4vrk.react.command.argument.LocalArgument;
import com.g4vrk.react.command.builder.CommandBuilderFactory;
import com.g4vrk.react.history.printer.InferenceHistoryPrinter;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.Selector;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;
import org.jetbrains.annotations.NotNull;

public final class HistoryArgument extends LocalArgument {

    private final CommandBuilderFactory builderFactory;

    private final InferenceHistoryPrinter printer;
    private final PlayerRegistry playerRegistry;

    public HistoryArgument(
            @NotNull CommandBuilderFactory builderFactory,
            @NotNull InferenceHistoryPrinter printer,
            @NotNull PlayerRegistry playerRegistry
    ) {
        this.builderFactory = builderFactory;
        this.printer = printer;
        this.playerRegistry = playerRegistry;
    }

    @Override
    public @NotNull Command.Builder<CommandSender> build() {
        return builderFactory.create()
                .literal("history")
                .optional("player", SinglePlayerSelectorParser.singlePlayerSelectorParser())
                .handler(context -> {
                    final CommandSender sender = context.sender();

                    final SinglePlayerSelector selector = context.getOrDefault("player", null);
                    final Player target = selector.single();

                    final ReactPlayer reactPlayer;

                    //noinspection ConstantValue
                    if (selector == null) {

                        if (!(sender instanceof Player player)) return;

                        reactPlayer = this.playerRegistry.getPlayer(player.getUniqueId());

                    } else {

                        reactPlayer = this.playerRegistry.getPlayer(target.getUniqueId());

                    }

                    if (reactPlayer != null) {

                        this.printer.print(sender, reactPlayer);

                    }
                });
    }
}
