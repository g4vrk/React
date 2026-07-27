package com.g4vrk.react.ml.aim;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class MLResult {

    private static final double UNAVAILABLE_PROBABILITY = -1.0D;

    double probability;
    double confidence;

    public boolean isAvailable() {
        return probability >= 0.0D;
    }

    public boolean hasConfidence() {
        return !Double.isNaN(confidence);
    }

    public static @NotNull MLResult unavailable() {
        return new MLResult(UNAVAILABLE_PROBABILITY, Double.NaN);
    }
}