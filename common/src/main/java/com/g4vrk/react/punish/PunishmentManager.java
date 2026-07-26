package com.g4vrk.react.punish;

import com.g4vrk.functionalActions.ExecutableAction;
import com.g4vrk.functionalActions.list.ExecutableActionList;
import com.g4vrk.functionalActions.parser.ActionParser;
import com.g4vrk.functionalActions.parser.impl.SimpleActionParser;
import com.g4vrk.functionalActions.registry.ActionRegistry;
import com.g4vrk.functionalActions.registry.impl.SimpleActionRegistry;
import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.alert.printer.AlertPrinter;
import com.g4vrk.react.api.task.runner.TaskRunner;
import com.g4vrk.react.api.task.schedule.TickSchedule;
import com.g4vrk.react.check.Check;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class PunishmentManager {

    private static final char PERIOD_SEPARATOR = ';';

    private final Logger logger;
    private final Server server;
    private final TaskRunner taskRunner;
    private final AlertPrinter alertPrinter;

    private final ActionRegistry<Context> actionRegistry = new SimpleActionRegistry<>(true);
    private final ActionParser<Context> actionParser = new SimpleActionParser<>(this.actionRegistry);

    private final Map<String, List<Rule>> rulesByCheck = new Object2ObjectOpenHashMap<>();

    public PunishmentManager(
            final @NotNull Config config,
            final @NotNull Logger logger,
            final @NotNull Server server,
            final @NotNull TaskRunner taskRunner,
            final @NotNull AlertPrinter alertPrinter
    ) {
        this.logger = logger;
        this.server = server;
        this.taskRunner = taskRunner;
        this.alertPrinter = alertPrinter;

        this.registerDefaultActions();
        this.parse(config.node("punishments"));
    }

    private void registerDefaultActions() {

        this.actionRegistry.register((ctx, args) -> {

            final String text = (args == null || args.isBlank())
                    ? "VL " + ctx.vl()
                    : args;

            this.alertPrinter.print(
                    ctx.check().getPlayer(),
                    ctx.check().getName(),
                    Component.text(text).color(NamedTextColor.RED)
            );

        }, "alert");

        this.actionRegistry.register((ctx, args) -> this.logger.info(
                "[Punishment] {} reached VL {} on {}",
                ctx.check().getPlayer().getName(), ctx.vl(), ctx.check().getName()
        ), "log");

        this.actionRegistry.register((ctx, args) -> ctx.check().resetViolations(),
                "reset-vl", "reset");

        this.actionRegistry.register((ctx, args) -> {

            if (args == null || args.isBlank()) {
                return;
            }

            this.taskRunner.runTask(
                    () -> this.server.dispatchCommand(this.server.getConsoleSender(), args),
                    TickSchedule.instant()
            );

        }, "console", "console-command", "cmd");
    }

    private void parse(
            final @NotNull ConfigurationNode punishmentsNode
    ) {

        if (punishmentsNode.virtual()) {
            logger.warn("punishments.yml does not contain a 'punishments' section, no punishments will be applied");
            return;
        }

        for (final Map.Entry<Object, ? extends ConfigurationNode> groupEntry
                : punishmentsNode.childrenMap().entrySet()) {

            final String groupName = String.valueOf(groupEntry.getKey());
            final ConfigurationNode groupNode = groupEntry.getValue();

            final List<String> checkNames;
            try {
                checkNames = groupNode.node("checks").getList(String.class);
            } catch (final Exception ex) {
                logger.warn("Could not read the 'checks' list of punishment group '{}'", groupName, ex);
                continue;
            }

            if (checkNames == null || checkNames.isEmpty()) {
                logger.warn("Punishment group '{}' does not define any checks, skipping", groupName);
                continue;
            }

            final List<Rule> rules = new ObjectArrayList<>();

            for (final Map.Entry<Object, ? extends ConfigurationNode> actionEntry
                    : groupNode.node("actions").childrenMap().entrySet()) {

                final Rule rule = this.parseRule(
                        groupName,
                        String.valueOf(actionEntry.getKey()),
                        actionEntry.getValue()
                );

                if (rule != null) {
                    rules.add(rule);
                }
            }

            if (rules.isEmpty()) {
                continue;
            }

            rules.sort(null);

            for (final String checkName : checkNames) {
                this.rulesByCheck
                        .computeIfAbsent(checkName.toLowerCase(Locale.ROOT), key -> new ObjectArrayList<>())
                        .addAll(rules);
            }
        }

        if (!this.rulesByCheck.isEmpty()) {
            logger.info("Loaded punishments for {} check(s): {}",
                    this.rulesByCheck.size(),
                    String.join(", ", this.rulesByCheck.keySet())
            );
        }
    }

    private @Nullable Rule parseRule(
            final @NotNull String groupName,
            final @NotNull String rawKey,
            final @NotNull ConfigurationNode actionsNode
    ) {

        final String key = rawKey.trim();
        final int separator = key.indexOf(PERIOD_SEPARATOR);

        final int threshold;
        int period = 0;

        try {

            if (separator == -1) {
                threshold = Integer.parseInt(key);
            } else {
                threshold = Integer.parseInt(key.substring(0, separator).trim());
                period = Integer.parseInt(key.substring(separator + 1).trim());
            }

        } catch (final NumberFormatException ex) {
            logger.warn(
                    "Invalid punishment threshold '{}' in group '{}', expected '<vl>' or '<vl>;<period>'",
                    rawKey, groupName
            );
            return null;
        }

        if (threshold <= 0) {
            logger.warn("Punishment threshold '{}' in group '{}' must be positive", rawKey, groupName);
            return null;
        }

        if (separator != -1 && period <= 0) {
            logger.warn(
                    "Invalid repeat period in threshold '{}' of group '{}', must be positive; treating as one-time",
                    rawKey, groupName
            );
            period = 0;
        }

        final List<String> actionStrings;
        try {
            actionStrings = actionsNode.getList(String.class);
        } catch (final Exception ex) {
            logger.warn("Could not read actions of threshold '{}' in group '{}'", rawKey, groupName, ex);
            return null;
        }

        if (actionStrings == null || actionStrings.isEmpty()) {
            return null;
        }

        final List<ExecutableAction<? super Context>> actions = new ObjectArrayList<>(actionStrings.size());

        for (final String rawAction : actionStrings) {

            final ExecutableAction<? super Context> action = this.actionParser.parse(rawAction);

            if (action == null) {
                logger.warn(
                        "Unknown punishment action '{}' (threshold '{}', group '{}')",
                        rawAction, rawKey, groupName
                );
                continue;
            }

            actions.add(action);
        }

        if (actions.isEmpty()) {
            return null;
        }

        return new Rule(threshold, period, new ExecutableActionList<>(actions));
    }

    public void handleFail(
            final @NotNull Check check,
            final double oldViolations,
            final double newViolations
    ) {

        if (newViolations <= oldViolations) {
            return;
        }

        final List<Rule> rules = this.rulesByCheck.get(check.getName().toLowerCase(Locale.ROOT));

        if (rules == null) {
            return;
        }

        for (final Rule rule : rules) {

            final int triggeredVl = findTriggerPoint(rule, oldViolations, newViolations);

            if (triggeredVl != -1) {
                this.execute(check, rule, triggeredVl);
            }

        }

    }

    private static int findTriggerPoint(
            final @NotNull Rule rule,
            final double oldVl,
            final double newVl
    ) {

        final int first = rule.threshold();

        if (newVl < first) {
            return -1;
        }

        if (!rule.repeating()) {
            return oldVl < first ? first : -1;
        }

        final int period = rule.period();

        final long steps = (long) Math.floor((newVl - first) / period);
        final long lastPoint = first + steps * period;

        return oldVl < lastPoint ? (int) lastPoint : -1;
    }

    private void execute(
            final @NotNull Check check,
            final @NotNull Rule rule,
            final int vl
    ) {

        final String playerName = check.getPlayer().getName();
        final String checkName = check.getName();

        final Context context = new Context(check, vl);

        final Function<String, String> placeholders = args -> args == null ? null : args
                .replace("{player}", playerName)
                .replace("{check}", checkName)
                .replace("{vl}", String.valueOf(vl));

        for (final ExecutableAction<? super Context> action : rule.actions()) {

            try {
                action.execute(context, placeholders);
            } catch (final Throwable th) {
                this.logger.error(
                        "Could not execute punishment action for {} ({}, VL {})",
                        playerName, checkName, vl, th
                );
            }

        }

    }

    public record Context(
            @NotNull Check check,
            int vl
    ) {}

    private record Rule(
            int threshold,
            int period,
            @NotNull ExecutableActionList<Context> actions
    ) implements Comparable<Rule> {

        private boolean repeating() {
            return this.period > 0;
        }

        @Override
        public int compareTo(final @NotNull Rule other) {
            return Integer.compare(this.threshold, other.threshold);
        }
    }
}