package com.g4vrk.react.ml.server.settings;

import com.g4vrk.react.ml.http.model.HttpClientSettings;
import lombok.Getter;
import okhttp3.ConnectionPool;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

@Getter
public final class MLClientSettings extends HttpClientSettings {

    private final String serverUrl;
    private final String apiKey;

    public MLClientSettings(
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout,
            @NotNull Duration writeTimeout,
            @NotNull Duration callTimeout,
            boolean retryOnFailure,
            @NotNull ConnectionPool connectionPool,
            @NotNull String serverUrl,
            @NotNull String apiKey
    ) {
        super(connectTimeout, readTimeout, writeTimeout, callTimeout, retryOnFailure, connectionPool);
        this.serverUrl = serverUrl;
        this.apiKey = apiKey;
    }
}
