package com.g4vrk.react.history.entry;

import com.g4vrk.react.check.Check;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static java.lang.System.currentTimeMillis;

@Getter
public final class HistoryEntry {

    private final long timestamp;

    private final Check check;

    private final double probability;

    private final double confidence;

    public HistoryEntry(
            @NotNull Check check,
            double probability,
            double confidence
    ) {
        this.timestamp = currentTimeMillis();
        this.check = check;
        this.probability = probability;
        this.confidence = confidence;
    }

}