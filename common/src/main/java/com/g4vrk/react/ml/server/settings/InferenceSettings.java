package com.g4vrk.react.ml.server.settings;

import com.g4vrk.react.ml.auth.AuthSettings;
import com.g4vrk.react.ml.http.model.HttpClientSettings;
import lombok.Getter;
import okhttp3.ConnectionPool;
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
            @NotNull Duration readTimeout,
            @NotNull Duration writeTimeout,
            @NotNull Duration callTimeout,
            boolean retryOnFailure,
            @NotNull ConnectionPool connectionPool,
            boolean enabled,
            @NotNull InferenceEndpointSettings endpoint,
            @NotNull InferenceRequestSettings request,
            @NotNull InferenceResponseSettings response,
            @NotNull AuthSettings auth
    ) {
        super(connectTimeout, readTimeout, writeTimeout, callTimeout, retryOnFailure, connectionPool);
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.request = request;
        this.response = response;
        this.auth = auth;
    }
}
