package com.g4vrk.react.history.entry;

import com.g4vrk.react.check.Check;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static java.lang.System.currentTimeMillis;

@Getter
public final class InferenceHistoryEntry {

    private final long timestamp;

    private final Check check;

    private final double probability;

    private final double confidence;

    public InferenceHistoryEntry(
            @NotNull Check check,
            double probability,
            double confidence
    ) {
        this(currentTimeMillis(), check, probability, confidence);
    }

    public InferenceHistoryEntry(
            long timestamp,
            @NotNull Check check,
            double probability,
            double confidence
    ) {
        this.timestamp = timestamp;
        this.check = check;
        this.probability = probability;
        this.confidence = confidence;
    }

}
