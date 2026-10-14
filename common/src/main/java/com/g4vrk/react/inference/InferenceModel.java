package com.g4vrk.react.inference;

import java.util.Locale;

public enum InferenceModel {
    FLOWER(8),
    UNIVERSE(44);

    private final int inputWidth;

    InferenceModel(int inputWidth) {
        this.inputWidth = inputWidth;
    }

    public int inputWidth() {
        return inputWidth;
    }

    public static InferenceModel parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
