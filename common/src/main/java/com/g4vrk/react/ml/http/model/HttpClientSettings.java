package com.g4vrk.react.ml.http.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

@AllArgsConstructor @Getter
@ToString @EqualsAndHashCode
public class HttpClientSettings {

    private final @NotNull Duration connectTimeout;
    private final @NotNull Duration readTimeout;
    private final @NotNull Duration writeTimeout;
    private final @NotNull Duration callTimeout;
    private final boolean retryOnFailure;
    private final @NotNull ConnectionPool connectionPool;

    public @NotNull OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(writeTimeout)
                .callTimeout(callTimeout)
                .retryOnConnectionFailure(retryOnFailure)
                .connectionPool(connectionPool)
                .build();
    }
}
