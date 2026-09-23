package com.g4vrk.react.history.printer;

import com.g4vrk.config.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import com.g4vrk.react.player.ReactPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class InferenceHistoryPrinter implements ReloadObserver {

    private final Function<String, Component> serializer;
    private final ValueColorResolver probabilityColorResolver = new ProbabilityColorResolver();

    private List<String> header;
    private List<String> entryFormat;
    private List<String> empty;
    private List<String> footer;
    private int printEntries;

    public InferenceHistoryPrinter(@NotNull Function<String, Component> serializer) {
        this.serializer = serializer;
        reload();
    }

    public void reload() {
        onReload(React.INSTANCE.getHistoryConfig());
    }

    @Override
    public void onReload(@NotNull Config config) {
        try {
            header = loadFormat(config, "header");
            entryFormat = loadFormat(config, "entry-format");
            empty = loadFormat(config, "empty");
            footer = loadFormat(config, "footer");
        } catch (SerializationException ex) {
            throw new RuntimeException(ex);
        }
        printEntries = Math.max(1, config.node("history", "inference", "format", "print-entries").getInt(18));
    }

    private @NotNull List<String> loadFormat(@NotNull Config config, @NotNull String key)
            throws SerializationException {
        return config.node("history", "inference", "format", key)
                .getList(String.class, Collections.emptyList());
    }

    public void print(@NotNull Audience receiver, @NotNull ReactPlayer player) {
        print(receiver, player, 1);
    }

    public void print(@NotNull Audience receiver, @NotNull ReactPlayer player, int page) {
        final InferenceHistoryEntry[] history = player.inferenceHistory.entries();
        final int totalPages = Math.max(1, (int) Math.ceil((double) history.length / printEntries));
        page = Math.max(1, Math.min(page, totalPages));
        final double average = player.inferenceStatistic.averageProbability();
        final Function<String, String> common = commonProcessor(player, history.length, page, totalPages, average);

        send(receiver, header, common, average, null);
        if (history.length == 0) {
            send(receiver, empty, common, average, null);
        } else {
            final int start = history.length - 1 - (page - 1) * printEntries;
            final int end = Math.max(-1, start - printEntries);
            for (int index = start; index > end; index--) {
                send(receiver, entryFormat, common, average, history[index]);
            }
        }
        send(receiver, footer, common, average, null);
    }

    private @NotNull Function<String, String> commonProcessor(
            @NotNull ReactPlayer player, int size, int page, int totalPages, double average
    ) {
        return text -> text
                .replace("{player}", player.getName())
                .replace("{size}", String.valueOf(size))
                .replace("{page:previous}", String.valueOf(Math.max(1, page - 1)))
                .replace("{page:current}", String.valueOf(page))
                .replace("{page:next}", String.valueOf(Math.min(totalPages, page + 1)))
                .replace("{page:max}", String.valueOf(totalPages))
                .replace("{avg-probability}", String.valueOf(average));
    }

    private void send(
            @NotNull Audience receiver,
            @NotNull List<String> lines,
            @NotNull Function<String, String> common,
            double average,
            InferenceHistoryEntry entry
    ) {
        for (String line : lines) {
            String text = common.apply(line);
            if (entry != null) {
                text = text.replace("{probability}", String.valueOf(entry.getProbability()))
                        .replace("{check}", entry.getCheck().getName());
            }
            Component component = serializer.apply(text);
            component = replaceColored(component, "{avg-probability:colored}", average);
            if (entry != null) {
                component = replaceColored(component, "{probability:colored}", entry.getProbability());
            }
            receiver.sendMessage(component);
        }
    }

    private @NotNull Component replaceColored(
            @NotNull Component component, @NotNull String placeholder, double probability
    ) {
        final Component colored = Component.text(probability)
                .color(probabilityColorResolver.resolve(probability));
        return component.replaceText(builder -> builder.matchLiteral(placeholder).replacement(colored));
    }
}
