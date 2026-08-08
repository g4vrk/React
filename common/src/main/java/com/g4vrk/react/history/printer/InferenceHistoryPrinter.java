package com.g4vrk.react.history.printer;

import com.g4vrk.fastTextFormatter.TextFormatter;
import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import com.g4vrk.react.player.model.ReactPlayer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.List;

public class InferenceHistoryPrinter implements ReloadObserver {

    private final TextFormatter textFormatter;

    private final ValueColorResolver colorResolver;

    private Component header;
    private Component entryFormat;
    private Component empty;
    private Component footer;

    private int printEntries;

    public InferenceHistoryPrinter(
            @NotNull TextFormatter textFormatter
    ) {

        this.textFormatter = textFormatter;
        this.colorResolver = new ProbabilityColorResolver();

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
            final @NotNull ReactPlayer reactPlayer
    ) {

        receiver.sendMessage(this.header);

        final List<InferenceHistoryEntry> history = new ObjectArrayList<>(reactPlayer.inferenceHistory.entries());

        int printedEntries = 0;

        for (final InferenceHistoryEntry entry : history) {

            if (history.isEmpty()) {
                receiver.sendMessage(empty);
                break;
            }

            if (printedEntries >= this.printEntries) break;

            final double probability = entry.getProbability();
            final double confidence = entry.getConfidence();

            Component component = entryFormat;
            component = replace(component, "probability", Component.text(probability).color(colorResolver.resolve(probability)));
            component = replace(component, "confidence", Component.text(confidence).color(colorResolver.resolve(confidence)));

            receiver.sendMessage(component);

            printedEntries++;

        }

        receiver.sendMessage(this.footer);

    }

    private @NotNull Component replace(
            final @NotNull Component component,
            final @NotNull String placeholder,
            final @NotNull Component replacement
    ) {
        return component.replaceText(builder ->
                builder
                        .matchLiteral('{' + placeholder + '}')
                        .replacement(replacement)
        );
    }
}