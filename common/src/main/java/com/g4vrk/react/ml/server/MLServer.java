package com.g4vrk.react.ml.server;

import com.g4vrk.react.ml.auth.AuthApplier;
import com.g4vrk.react.ml.auth.AuthSettings;
import com.g4vrk.react.ml.auth.type.AuthType;
import com.g4vrk.react.ml.http.model.HttpRequest;
import com.g4vrk.react.ml.server.settings.InferenceEndpointSettings;
import com.g4vrk.react.ml.server.settings.InferenceRequestSettings;
import com.g4vrk.react.ml.server.settings.InferenceResponseSettings;
import com.g4vrk.react.ml.server.settings.InferenceSettings;
import lombok.Getter;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Map;

@Getter
public final class MLServer {

    private final OkHttpClient client;
    private final AuthApplier authApplier;

    private final String baseUrl;
    private final InferenceEndpointSettings endpoint;
    private final InferenceRequestSettings requestSettings;
    private final InferenceResponseSettings responseSettings;

    private final boolean enabled;

    public MLServer(
            final @NotNull Logger logger,
            final @NotNull InferenceSettings settings
    ) {
        this.client = settings.buildClient();
        this.enabled = settings.isEnabled();

        this.endpoint = settings.getEndpoint();
        this.requestSettings = settings.getRequest();
        this.responseSettings = settings.getResponse();

        this.baseUrl = normalizeUrl(endpoint.getBaseUrl());
        this.authApplier = new AuthApplier(settings.getAuth());

        if (enabled) {
            validate(logger, settings.getAuth());
        }
    }

    public @NotNull Call newCall(
            final @NotNull HttpRequest httpRequest
    ) {
        final String rawUrl = baseUrl + endpoint.getPath();
        final HttpUrl parsedUrl = HttpUrl.parse(rawUrl);

        if (parsedUrl == null) {
            throw new IllegalStateException(
                    "Invalid ML inference endpoint URL: '" + rawUrl + "'. " +
                            "Check inference.endpoint.base-url and inference.endpoint.path"
            );
        }

        final HttpUrl url = authApplier.applyToUrl(parsedUrl);

        final Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .method(httpRequest.getMethod(), httpRequest.getBody());

        authApplier.applyToHeaders(requestBuilder);

        return this.client.newCall(requestBuilder.build());
    }

    public void callAsync(
            final @NotNull HttpRequest request,
            final @NotNull Callback callback
    ) {
        this.newCall(request).enqueue(callback);
    }

    public void augmentPayload(final @NotNull Map<String, Object> payload) {
        this.authApplier.applyToPayload(payload);
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

    private void validate(
            final @NotNull Logger logger,
            final @NotNull AuthSettings auth
    ) {
        if (HttpUrl.parse(baseUrl + endpoint.getPath()) == null) {
            logger.warn(
                    "ML inference endpoint '{}{}' is not a valid URL, requests will fail",
                    baseUrl, endpoint.getPath()
            );
        }

        if (!auth.isEnabled() || auth.getType() == AuthType.NONE) {
            return;
        }

        switch (auth.getType()) {
            case HEADER -> warnIfBlank(logger, "auth.header.name", auth.getHeaderName());

            case QUERY -> warnIfBlank(logger, "auth.query.parameter", auth.getQueryParameter());

            case BODY -> warnIfBlank(logger, "auth.body.field", auth.getBodyField());
        }
    }

    private static void warnIfBlank(
            final @NotNull Logger logger,
            final @NotNull String key,
            final String value
    ) {
        if (value == null || value.isBlank()) {
            logger.warn("ML inference config '{}' is blank, requests may be rejected", key);
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
