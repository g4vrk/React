package com.g4vrk.react.ml.aim;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class MLResult {

    private static final double DEFAULT_VALUE = -1.0D;

    double probability;
    double confidence;

    public boolean isAvailable() {
        return probability >= 0.0D;
    }

    public boolean hasConfidence() {
        return confidence >= 0.0D;
    }

    public static @NotNull MLResult unavailable() {
        return new MLResult(DEFAULT_VALUE, DEFAULT_VALUE);
    }
}