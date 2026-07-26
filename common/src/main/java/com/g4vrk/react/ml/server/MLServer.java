package com.g4vrk.react.ml.server;

import com.g4vrk.react.ml.http.model.HttpRequest;
import com.g4vrk.react.ml.server.settings.MLClientSettings;
import lombok.AccessLevel;
import lombok.Getter;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

@Getter
public class MLServer {

    @Getter(AccessLevel.NONE)
    private final OkHttpClient client;

    private final String serverUrl;
    private final String apiKey;

    private final boolean enabled;

    public MLServer(
            @NotNull MLClientSettings settings
    ) {
        this.client = settings.buildClient();
        this.serverUrl = normalizeUrl(settings.getServerUrl());
        this.apiKey = settings.getApiKey();
        this.enabled = settings.isEnabled();
    }

    public @NotNull Call newCall(
            final @NotNull Request request
    ) {
        return this.client.newCall(request);
    }

    public @NotNull Call newCall(
            final @NotNull HttpRequest httpRequest
    ) {
        final Request request = new Request.Builder()
                .url(serverUrl + httpRequest.getPath())
                .method(httpRequest.getMethod(), httpRequest.getBody())
                .addHeader("X-Subscription-Token", apiKey)
                .build();

        return this.newCall(request);
    }

    public void callAsync(
            final @NotNull HttpRequest request,
            final @NotNull Callback callback
    ) {
        this.newCall(request).enqueue(callback);
    }

    public void shutdown() {
        try {
            this.client.dispatcher().executorService().shutdown();
        } catch (final Throwable ignored) {
        }

        try {
            this.client.connectionPool().evictAll();
        } catch (final Throwable ignored) {
        }

        try {
            final Cache cache = this.client.cache();

            if (cache != null) {
                cache.close();
            }
        } catch (final Throwable ignored) {
        }
    }

    private static @NotNull String normalizeUrl(
            final @NotNull String rawUrl
    ) {
        String url = rawUrl.trim();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }
}
