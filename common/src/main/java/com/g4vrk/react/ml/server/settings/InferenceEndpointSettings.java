package com.g4vrk.react.ml.server.settings;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class InferenceEndpointSettings {

    @NotNull String baseUrl;
    @NotNull String path;
}
