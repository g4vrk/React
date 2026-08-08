package com.g4vrk.react.placeholder.provider.prefixed.impl;

import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import com.g4vrk.react.placeholder.provider.prefixed.PrefixedPlaceholderProvider;
import com.g4vrk.react.player.model.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import org.jetbrains.annotations.NotNull;

public class InferenceHistoryPlaceholderProvider extends PrefixedPlaceholderProvider {

    public InferenceHistoryPlaceholderProvider(
            @NotNull PlayerRegistry registry
    ) {

        super("inference-history");

        final ValueColorResolver colorResolver = new ProbabilityColorResolver();

        super.replacement("size", (offlinePlayer, placeholder) -> {

            final ReactPlayer player = registry.getPlayer(offlinePlayer.getUniqueId());

            if (player != null) return String.valueOf(player.inferenceHistory.size());

            return placeholder;

        });

        super.replacement("entry", (offlinePlayer, placeholder) -> {

            final ReactPlayer player = registry.getPlayer(offlinePlayer.getUniqueId());

            if (player == null) {
                return placeholder;
            }

            final String prefix = "entry.";

            if (!placeholder.startsWith(prefix)) {
                return placeholder;
            }

            final String path = placeholder.substring(prefix.length());
            final int separator = path.indexOf('.');

            if (separator == -1) {
                return placeholder;
            }

            final int index;

            try {
                index = Integer.parseInt(path, 0, separator, 10);
            } catch (NumberFormatException ignored) {
                return placeholder;
            }

            final InferenceHistoryEntry entry = player.inferenceHistory.get(index);

            if (entry == null) {
                return placeholder;
            }

            return switch (path.substring(separator + 1)) {
                case "probability" -> {

                    final double probability = entry.getProbability();

                    yield Double.toString(probability);

                }

                case "probability:colored" -> {

                    final double probability = entry.getProbability();

                    yield "<" + colorResolver.resolve(probability).asHexString() + ">" + probability;

                }

                case "confidence" -> {

                    final double confidence = entry.getConfidence();

                    yield Double.toString(confidence);

                }

                case "confidence:colored" -> {

                    final double confidence = entry.getConfidence();

                    yield "<" + colorResolver.resolve(confidence).asHexString() + ">" + confidence;

                }

                case "check" -> entry.getCheck().getName();
                default -> placeholder;
            };
        });

        super.replacement("history.avg-probability", (offlinePlayer, placeholder) -> {

            final ReactPlayer player =
                    registry.getPlayer(offlinePlayer.getUniqueId());

            if (player == null) {
                return placeholder;
            }

            final int size = player.inferenceHistory.size();

            if (size == 0) {
                return "0";
            }

            double sum = 0.0D;

            for (int i = 0; i < size; i++) {
                final InferenceHistoryEntry entry =
                        player.inferenceHistory.get(i);

                if (entry != null) {
                    sum += entry.getProbability();
                }
            }

            return Double.toString(sum / size);
        });

        super.replacement("avg-probability:colored", (offlinePlayer, placeholder) -> {

            final ReactPlayer player =
                    registry.getPlayer(offlinePlayer.getUniqueId());

            if (player == null) {
                return placeholder;
            }

            final int size = player.inferenceHistory.size();

            if (size == 0) {
                return "0";
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

            return "<" + colorResolver.resolve(average).asHexString() + ">" + average;
        });

        super.replacement("avg-confidence", (offlinePlayer, placeholder) -> {

            final ReactPlayer player =
                    registry.getPlayer(offlinePlayer.getUniqueId());

            if (player == null) {
                return placeholder;
            }

            final int size = player.inferenceHistory.size();

            if (size == 0) {
                return "0";
            }

            double sum = 0.0D;

            for (int i = 0; i < size; i++) {
                final InferenceHistoryEntry entry =
                        player.inferenceHistory.get(i);

                if (entry != null) {
                    sum += entry.getConfidence();
                }
            }

            return Double.toString(sum / size);
        });

        super.replacement("avg-confidence:colored", (offlinePlayer, placeholder) -> {

            final ReactPlayer player =
                    registry.getPlayer(offlinePlayer.getUniqueId());

            if (player == null) {
                return placeholder;
            }

            final int size = player.inferenceHistory.size();

            if (size == 0) {
                return "0";
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

            return "<" + colorResolver.resolve(average).asHexString() + ">" + average;
        });

    }

}
