package com.g4vrk.react.inference.server.settings;

import com.g4vrk.react.inference.auth.AuthSettings;
import com.g4vrk.react.inference.http.model.HttpClientSettings;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

@Getter
public final class InferenceSettings extends HttpClientSettings {

    private final boolean enabled;

    private final @NotNull InferenceEndpointSettings endpoint;
    private final @NotNull InferenceRequestSettings request;
    private final @NotNull InferenceResponseSettings response;
    private final @NotNull AuthSettings auth;

    public InferenceSettings(
            @NotNull Duration connectTimeout,
            @NotNull Duration callTimeout,
            boolean enabled,
            @NotNull InferenceEndpointSettings endpoint,
            @NotNull InferenceRequestSettings request,
            @NotNull InferenceResponseSettings response,
            @NotNull AuthSettings auth
    ) {
        super(connectTimeout, callTimeout);
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.request = request;
        this.response = response;
        this.auth = auth;
    }
}
