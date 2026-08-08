package com.g4vrk.react.history.printer;

import com.g4vrk.fastTextFormatter.TextFormatter;
import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ConfidenceColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import com.g4vrk.react.player.ReactPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.List;

public class InferenceHistoryPrinter implements ReloadObserver {

    private final TextFormatter textFormatter;

    private final ValueColorResolver confidenceColorResolver;
    private final ValueColorResolver probabilityColorResolver;

    private Component header;
    private Component entryFormat;
    private Component empty;
    private Component footer;

    private int printEntries;

    public InferenceHistoryPrinter(
            @NotNull TextFormatter textFormatter
    ) {

        this.textFormatter = textFormatter;
        this.confidenceColorResolver = new ConfidenceColorResolver();
        this.probabilityColorResolver = new ProbabilityColorResolver();

        this.reload();

    }

    public void reload() {

        final Config config = React.INSTANCE.getHistoryConfig();

        this.onReload(config);

    }

    @Override
    public void onReload(@NotNull Config config) {

        try {

            this.header = formatList(
                    config.node(
                            "history",
                            "inference",
                            "format",
                            "header"
                    ).getList(String.class, Collections.emptyList())
            );

            this.entryFormat = formatList(
                    config.node(
                            "history",
                            "inference",
                            "format",
                            "entry-format"
                    ).getList(String.class, Collections.emptyList())
            );

            this.empty = formatList(
                    config.node(
                            "history",
                            "inference",
                            "format",
                            "empty"
                    ).getList(String.class, Collections.emptyList())
            );

            this.footer = formatList(
                    config.node(
                            "history",
                            "inference",
                            "format",
                            "footer"
                    ).getList(String.class, Collections.emptyList())
            );

        } catch (final SerializationException ex) {
            throw new RuntimeException(ex);
        }

        this.printEntries = config.node(
                "history",
                "inference",
                "format",
                "print-entries"
        ).getInt(18);
    }

    private @NotNull Component formatList(
            final @NotNull List<String> lines
    ) {
        return textFormatter.format(String.join("<newline>", lines));
    }

    public void print(
            final @NotNull Audience receiver,
            final @NotNull ReactPlayer player
    ) {
        final InferenceHistoryEntry[] history =
                player.inferenceHistory.entries();

        receiver.sendMessage(
                replaceCommon(
                        this.header,
                        player,
                        history
                )
        );

        if (history.length == 0) {
            receiver.sendMessage(
                    replaceCommon(
                            this.empty,
                            player,
                            history
                    )
            );

            receiver.sendMessage(
                    replaceCommon(
                            this.footer,
                            player,
                            history
                    )
            );

            return;
        }

        final int end = Math.max(
                0,
                history.length - this.printEntries
        );

        for (int i = history.length - 1; i >= end; i--) {
            Component component = replaceCommon(
                    this.entryFormat,
                    player,
                    history
            );

            component = replaceEntry(
                    component,
                    history[i]
            );

            receiver.sendMessage(component);
        }

        receiver.sendMessage(
                replaceCommon(
                        this.footer,
                        player,
                        history
                )
        );
    }

    private @NotNull Component replaceCommon(
            @NotNull Component component,
            @NotNull ReactPlayer player,
            @NotNull InferenceHistoryEntry[] history
    ) {
        final double avgProbability = averageProbability(history);
        final double avgConfidence = averageConfidence(history);

        component = replace(
                component,
                "{player}",
                Component.text(player.getName())
        );

        component = replace(
                component,
                "{size}",
                Component.text(history.length)
        );

        component = replace(
                component,
                "{avg-probability}",
                Component.text(avgProbability)
        );

        component = replace(
                component,
                "{avg-probability:colored}",
                Component.text(avgProbability)
                        .color(probabilityColorResolver.resolve(avgProbability))
        );

        component = replace(
                component,
                "{avg-confidence}",
                Component.text(avgConfidence)
        );

        component = replace(
                component,
                "{avg-confidence:colored}",
                Component.text(avgConfidence)
                        .color(confidenceColorResolver.resolve(avgConfidence))
        );

        return component;
    }

    private @NotNull Component replaceEntry(
            @NotNull Component component,
            @NotNull InferenceHistoryEntry entry
    ) {
        final double probability = entry.getProbability();
        final double confidence = entry.getConfidence();

        component = replace(
                component,
                "{probability}",
                Component.text(probability)
        );

        component = replace(
                component,
                "{probability:colored}",
                Component.text(probability)
                        .color(probabilityColorResolver.resolve(probability))
        );

        component = replace(
                component,
                "{confidence}",
                Component.text(confidence)
        );

        component = replace(
                component,
                "{confidence:colored}",
                Component.text(confidence)
                        .color(confidenceColorResolver.resolve(confidence))
        );

        component = replace(
                component,
                "{check}",
                Component.text(entry.getCheck().getName())
        );

        return component;
    }

    private double averageProbability(
            @NotNull InferenceHistoryEntry[] history
    ) {
        if (history.length == 0) {
            return 0.0D;
        }

        double sum = 0.0D;

        for (final InferenceHistoryEntry entry : history) {
            sum += entry.getProbability();
        }

        return sum / history.length;
    }

    private double averageConfidence(
            @NotNull InferenceHistoryEntry[] history
    ) {
        if (history.length == 0) {
            return 0.0D;
        }

        double sum = 0.0D;

        for (final InferenceHistoryEntry entry : history) {
            sum += entry.getConfidence();
        }

        return sum / history.length;
    }

    private @NotNull Component replace(
            final @NotNull Component component,
            final @NotNull String placeholder,
            final @NotNull Component replacement
    ) {
        return component.replaceText(builder ->
                builder.matchLiteral(placeholder)
                        .replacement(replacement)
        );
    }
}