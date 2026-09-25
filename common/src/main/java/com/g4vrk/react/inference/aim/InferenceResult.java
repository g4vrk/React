package com.g4vrk.react.inference.aim;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class InferenceResult {

    private static final double DEFAULT_VALUE = -1.0D;

    double probability;

    public boolean isAvailable() {
        return probability >= 0.0D;
    }

    public static @NotNull InferenceResult unavailable() {
        return new InferenceResult(DEFAULT_VALUE);
    }
}
