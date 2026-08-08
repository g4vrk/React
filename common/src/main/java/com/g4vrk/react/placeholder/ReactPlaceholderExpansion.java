package com.g4vrk.react.placeholder;

import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ConfidenceColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import com.g4vrk.react.player.model.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReactPlaceholderExpansion extends PlaceholderExpansion {

    private final String id;
    private final String author;
    private final String version;

    private final PlayerRegistry registry;

    private final ValueColorResolver probabilityColorResolver = new ProbabilityColorResolver();
    private final ValueColorResolver confidenceColorResolver = new ConfidenceColorResolver();

    public ReactPlaceholderExpansion(
            @NotNull String id,
            @NotNull String author,
            @NotNull String version,
            @NotNull PlayerRegistry registry
    ) {
        this.id = id;
        this.author = author;
        this.version = version;
        this.registry = registry;
    }

    @Override
    public @NotNull String getIdentifier() {
        return id;
    }

    @Override
    public @NotNull String getAuthor() {
        return author;
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }


    @Override
    public @Nullable String onRequest(
            final OfflinePlayer offlinePlayer,
            final @NotNull String params
    ) {

        if (offlinePlayer == null) {
            return null;
        }

        final String prefix = "inference-history.";

        if (!params.startsWith(prefix)) {
            return null;
        }

        final ReactPlayer player =
                registry.getPlayer(offlinePlayer.getUniqueId());

        if (player == null) {
            return null;
        }

        final String path = params.substring(prefix.length());

        if (path.startsWith("entry.")) {

            final String entryPrefix = "entry.";

            final int separator = path.indexOf('.', entryPrefix.length());

            if (separator == -1) {
                return null;
            }

            final int index;

            try {
                index = Integer.parseInt(
                        path,
                        entryPrefix.length(),
                        separator,
                        10
                );
            } catch (NumberFormatException ignored) {
                return null;
            }

            final InferenceHistoryEntry entry =
                    player.inferenceHistory.get(index);

            if (entry == null) {
                return null;
            }

            return switch (path.substring(separator + 1)) {

                case "probability" -> {
                    final double probability = entry.getProbability();

                    yield Double.toString(probability);
                }

                case "probability:colored" -> {
                    final double probability = entry.getProbability();

                    yield "<" +
                            probabilityColorResolver.resolve(probability).asHexString() +
                            ">" +
                            probability;
                }

                case "confidence" -> {
                    final double confidence = entry.getConfidence();

                    yield Double.toString(confidence);
                }

                case "confidence:colored" -> {
                    final double confidence = entry.getConfidence();

                    yield "<" +
                            confidenceColorResolver.resolve(confidence).asHexString() +
                            ">" +
                            confidence;
                }

                case "check" -> entry.getCheck().getName();

                default -> null;
            };
        }

        return switch (path) {

            case "size" ->
                    Integer.toString(player.inferenceHistory.size());

            case "avg-probability" -> {

                final int size = player.inferenceHistory.size();

                if (size == 0) {
                    yield "0";
                }

                double sum = 0.0D;

                for (int i = 0; i < size; i++) {
                    final InferenceHistoryEntry entry =
                            player.inferenceHistory.get(i);

                    if (entry != null) {
                        sum += entry.getProbability();
                    }
                }

                yield Double.toString(sum / size);
            }

            case "avg-probability:colored" -> {

                final int size = player.inferenceHistory.size();

                if (size == 0) {
                    yield "0";
                }

                double sum = 0.0D;

                for (int i = 0; i < size; i++) {
                    final InferenceHistoryEntry entry =
                            player.inferenceHistory.get(i);

                    if (entry != null) {
                        sum += entry.getProbability();
                    }
                }

                final double average = sum / size;

                yield "<" +
                        probabilityColorResolver.resolve(average).asHexString() +
                        ">" +
                        average;
            }

            case "avg-confidence" -> {

                final int size = player.inferenceHistory.size();

                if (size == 0) {
                    yield "0";
                }

                double sum = 0.0D;

                for (int i = 0; i < size; i++) {
                    final InferenceHistoryEntry entry =
                            player.inferenceHistory.get(i);

                    if (entry != null) {
                        sum += entry.getConfidence();
                    }
                }

                yield Double.toString(sum / size);
            }

            case "avg-confidence:colored" -> {

                final int size = player.inferenceHistory.size();

                if (size == 0) {
                    yield "0";
                }

                double sum = 0.0D;

                for (int i = 0; i < size; i++) {
                    final InferenceHistoryEntry entry =
                            player.inferenceHistory.get(i);

                    if (entry != null) {
                        sum += entry.getConfidence();
                    }
                }

                final double average = sum / size;

                yield "<" +
                        confidenceColorResolver.resolve(average).asHexString() +
                        ">" +
                        average;
            }

            default -> null;
        };
    }
}