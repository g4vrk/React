package com.g4vrk.react.inference.server.settings;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class InferenceRequestSettings {

    @NotNull String playerNameField;
    @NotNull String rotationsField;
}
