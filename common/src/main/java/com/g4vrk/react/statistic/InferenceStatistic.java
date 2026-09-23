package com.g4vrk.react.statistic;

import com.g4vrk.react.history.InferenceHistory;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InferenceStatistic {

    private final InferenceHistory history;

    public InferenceStatistic(@NotNull InferenceHistory history) {
        this.history = history;
    }

    public int size() {
        return history.size();
    }

    public double averageProbability() {
        return calculate().averageProbability();
    }

    public double maxProbability() {
        return calculate().maxProbability();
    }

    public double minProbability() {
        return calculate().minProbability();
    }

    public @Nullable InferenceHistoryEntry latest() {
        return history.latest();
    }

    public @NotNull Result calculate() {
        final InferenceHistoryEntry[] entries = history.entries();
        if (entries.length == 0) {
            return Result.EMPTY;
        }

        double sum = 0.0D;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (final InferenceHistoryEntry entry : entries) {
            final double probability = entry.getProbability();
            sum += probability;
            max = Math.max(max, probability);
            min = Math.min(min, probability);
        }
        return new Result(sum / entries.length, max, min);
    }

    public record Result(double averageProbability, double maxProbability, double minProbability) {
        private static final Result EMPTY = new Result(0.0D, 0.0D, 0.0D);
    }
}
