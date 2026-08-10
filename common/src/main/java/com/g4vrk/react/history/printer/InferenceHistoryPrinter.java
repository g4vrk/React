package com.g4vrk.react.history.printer;

import com.g4vrk.fastTextFormatter.TextFormatter;
import com.g4vrk.fastTextFormatter.function.TextPreProcessor;
import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ConfidenceColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.statistic.InferenceStatistic;
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

    private String header;
    private String entryFormat;
    private String empty;
    private String footer;

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

            this.header = loadFormat(
                    config,
                    "header"
            );

            this.entryFormat = loadFormat(
                    config,
                    "entry-format"
            );

            this.empty = loadFormat(
                    config,
                    "empty"
            );

            this.footer = loadFormat(
                    config,
                    "footer"
            );

        } catch (final SerializationException ex) {
            throw new RuntimeException(ex);
        }

        this.printEntries = Math.max(
                1,
                config.node(
                        "history",
                        "inference",
                        "format",
                        "print-entries"
                ).getInt(18)
        );
    }

    private @NotNull String loadFormat(
            final @NotNull Config config,
            final @NotNull String key
    ) throws SerializationException {

        return String.join(
                "<newline>",
                config.node(
                        "history",
                        "inference",
                        "format",
                        key
                ).getList(String.class, Collections.emptyList())
        );

    }

    public void print(
            final @NotNull Audience receiver,
            final @NotNull ReactPlayer player
    ) {

        print(
                receiver,
                player,
                1
        );

    }

    public void print(
            final @NotNull Audience receiver,
            final @NotNull ReactPlayer player,
            int page
    ) {

        final InferenceHistoryEntry[] history =
                player.inferenceHistory.entries();

        final int totalPages = Math.max(
                1,
                (int) Math.ceil(
                        (double) history.length / this.printEntries
                )
        );

        page = Math.max(
                1,
                Math.min(
                        page,
                        totalPages
                )
        );

        final InferenceStatistic.Result statisticResult =
                player.inferenceStatistic.calculate();

        final double avgProbability =
                statisticResult.averageProbability();

        final double avgConfidence =
                statisticResult.averageConfidence();

        final TextPreProcessor commonProcessor = createCommonProcessor(
                player,
                history,
                page,
                totalPages,
                avgProbability,
                avgConfidence
        );

        receiver.sendMessage(
                formatCommon(
                        this.header,
                        commonProcessor,
                        avgProbability,
                        avgConfidence
                )
        );

        if (history.length == 0) {

            receiver.sendMessage(
                    formatCommon(
                            this.empty,
                            commonProcessor,
                            avgProbability,
                            avgConfidence
                    )
            );

            receiver.sendMessage(
                    formatCommon(
                            this.footer,
                            commonProcessor,
                            avgProbability,
                            avgConfidence
                    )
            );

            return;
        }

        final int start =
                history.length - 1
                        - ((page - 1) * this.printEntries);

        final int end = Math.max(
                -1,
                start - this.printEntries
        );

        for (int i = start; i > end; i--) {

            receiver.sendMessage(
                    formatEntry(
                            this.entryFormat,
                            commonProcessor,
                            history[i],
                            avgProbability,
                            avgConfidence
                    )
            );

        }

        receiver.sendMessage(
                formatCommon(
                        this.footer,
                        commonProcessor,
                        avgProbability,
                        avgConfidence
                )
        );
    }

    private @NotNull TextPreProcessor createCommonProcessor(
            final @NotNull ReactPlayer player,
            final @NotNull InferenceHistoryEntry[] history,
            final int currentPage,
            final int maxPage,
            final double avgProbability,
            final double avgConfidence
    ) {

        return text -> text
                .replace(
                        "{player}",
                        player.getName()
                )
                .replace(
                        "{size}",
                        String.valueOf(history.length)
                )
                .replace(
                        "{page:previous}",
                        String.valueOf(
                                Math.max(
                                        1,
                                        currentPage - 1
                                )
                        )
                )
                .replace(
                        "{page:current}",
                        String.valueOf(currentPage)
                )
                .replace(
                        "{page:next}",
                        String.valueOf(
                                Math.min(
                                        maxPage,
                                        currentPage + 1
                                )
                        )
                )
                .replace(
                        "{page:max}",
                        String.valueOf(maxPage)
                )
                .replace(
                        "{avg-probability}",
                        String.valueOf(avgProbability)
                )
                .replace(
                        "{avg-confidence}",
                        String.valueOf(avgConfidence)
                );

    }

    private @NotNull Component formatCommon(
            final @NotNull String input,
            final @NotNull TextPreProcessor commonProcessor,
            final double avgProbability,
            final double avgConfidence
    ) {

        Component component = this.textFormatter.formatWithPreProcessors(
                input,
                List.of(
                        commonProcessor
                )
        );

        component = replaceColoredCommon(
                component,
                avgProbability,
                avgConfidence
        );

        return component;
    }

    private @NotNull Component formatEntry(
            final @NotNull String input,
            final @NotNull TextPreProcessor commonProcessor,
            final @NotNull InferenceHistoryEntry entry,
            final double avgProbability,
            final double avgConfidence
    ) {

        final double probability =
                entry.getProbability();

        final double confidence =
                entry.getConfidence();

        final TextPreProcessor entryProcessor = text -> text
                .replace(
                        "{probability}",
                        String.valueOf(probability)
                )
                .replace(
                        "{confidence}",
                        String.valueOf(confidence)
                )
                .replace(
                        "{check}",
                        entry.getCheck().getName()
                );

        Component component = this.textFormatter.formatWithPreProcessors(
                input,
                List.of(
                        commonProcessor,
                        entryProcessor
                )
        );

        component = replaceColoredCommon(
                component,
                avgProbability,
                avgConfidence
        );

        component = replace(
                component,
                "{probability:colored}",
                Component.text(probability)
                        .color(
                                this.probabilityColorResolver.resolve(
                                        probability
                                )
                        )
        );

        component = replace(
                component,
                "{confidence:colored}",
                Component.text(confidence)
                        .color(
                                this.confidenceColorResolver.resolve(
                                        confidence
                                )
                        )
        );

        return component;
    }

    private @NotNull Component replaceColoredCommon(
            @NotNull Component component,
            final double avgProbability,
            final double avgConfidence
    ) {

        component = replace(
                component,
                "{avg-probability:colored}",
                Component.text(avgProbability)
                        .color(
                                this.probabilityColorResolver.resolve(
                                        avgProbability
                                )
                        )
        );

        component = replace(
                component,
                "{avg-confidence:colored}",
                Component.text(avgConfidence)
                        .color(
                                this.confidenceColorResolver.resolve(
                                        avgConfidence
                                )
                        )
        );

        return component;
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