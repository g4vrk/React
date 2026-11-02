package com.g4vrk.react.inference.http.model;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class HttpRequest {
    @NotNull String method;
    @NotNull String body;
}
