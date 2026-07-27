package com.g4vrk.react.ml.http.model;

import lombok.Value;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;

@Value
public class HttpRequest {
    @NotNull String method;
    @NotNull RequestBody body;
}