package com.g4vrk.react.ml.server.settings;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class InferenceResponseSettings {

    @NotNull String probabilityField;
    @NotNull String confidenceField;
}
