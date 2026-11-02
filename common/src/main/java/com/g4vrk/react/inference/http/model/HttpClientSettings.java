package com.g4vrk.react.inference.http.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.time.Duration;

@AllArgsConstructor @Getter
@ToString @EqualsAndHashCode
public class HttpClientSettings {

    private final @NotNull Duration connectTimeout;
    private final @NotNull Duration callTimeout;

    public @NotNull HttpClient buildClient() {
        final HttpClient.Builder builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (!connectTimeout.isZero() && !connectTimeout.isNegative()) {
            builder.connectTimeout(connectTimeout);
        }

        return builder.build();
    }
}
