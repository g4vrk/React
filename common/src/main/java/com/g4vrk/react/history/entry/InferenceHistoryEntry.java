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

    public InferenceHistoryEntry(
            @NotNull Check check,
            double probability
    ) {
        this.timestamp = currentTimeMillis();
        this.check = check;
        this.probability = probability;
    }

}