package com.g4vrk.react.statistic;

import com.g4vrk.react.history.InferenceHistory;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InferenceStatistic {

    private final InferenceHistory history;

    public InferenceStatistic(
            @NotNull InferenceHistory history
    ) {
        this.history = history;
    }

    public int size() {
        return history.size();
    }

    public double averageProbability() {
        return calculate().averageProbability;
    }

    public double averageConfidence() {
        return calculate().averageConfidence;
    }

    public double maxProbability() {
        return calculate().maxProbability;
    }

    public double minProbability() {
        return calculate().minProbability;
    }

    public double maxConfidence() {
        return calculate().maxConfidence;
    }

    public double minConfidence() {
        return calculate().minConfidence;
    }

    public @Nullable InferenceHistoryEntry latest() {
        return history.latest();
    }

    public @NotNull Result calculate() {

        final InferenceHistoryEntry[] entries = history.entries();

        if (entries.length == 0) {
            return Result.EMPTY;
        }

        double probabilitySum = 0.0D;
        double confidenceSum = 0.0D;

        double maxProbability = Double.NEGATIVE_INFINITY;
        double minProbability = Double.POSITIVE_INFINITY;

        double maxConfidence = Double.NEGATIVE_INFINITY;
        double minConfidence = Double.POSITIVE_INFINITY;

        for (final InferenceHistoryEntry entry : entries) {
            final double probability = entry.getProbability();
            final double confidence = entry.getConfidence();

            probabilitySum += probability;
            confidenceSum += confidence;

            if (probability > maxProbability) {
                maxProbability = probability;
            }

            if (probability < minProbability) {
                minProbability = probability;
            }

            if (confidence > maxConfidence) {
                maxConfidence = confidence;
            }

            if (confidence < minConfidence) {
                minConfidence = confidence;
            }
        }

        final int size = entries.length;

        return new Result(
                probabilitySum / size,
                confidenceSum / size,
                maxProbability,
                minProbability,
                maxConfidence,
                minConfidence
        );

    }

    public record Result(
            double averageProbability,
            double averageConfidence,
            double maxProbability,
            double minProbability,
            double maxConfidence,
            double minConfidence
    ) {

        private static final Result EMPTY = new Result(
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }
}