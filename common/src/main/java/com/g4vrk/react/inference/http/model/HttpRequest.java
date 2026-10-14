package com.g4vrk.react.inference.http.model;

import lombok.Value;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

@Value
public class HttpRequest {
    @NotNull String method;
    @NotNull RequestBody body;
    @NotNull Map<String, String> headers;

    public HttpRequest(String method, RequestBody body) {
        this(method, body, Map.of());
    }

    public HttpRequest(String method, RequestBody body, Map<String, String> headers) {
        this.method = method;
        this.body = body;
        this.headers = Map.copyOf(headers);
    }
}
