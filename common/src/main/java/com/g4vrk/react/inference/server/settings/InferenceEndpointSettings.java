package com.g4vrk.react.inference.server.settings;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class InferenceEndpointSettings {

    @NotNull String baseUrl;
    @NotNull String path;
    @NotNull String modelFamily;
    @NotNull String modelName;
}
