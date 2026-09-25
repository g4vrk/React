package com.g4vrk.react.inference.server.settings;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class InferenceResponseSettings {

    @NotNull String probabilityField;
}
