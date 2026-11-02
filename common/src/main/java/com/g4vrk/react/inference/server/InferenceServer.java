package com.g4vrk.react.inference.server;

import com.g4vrk.react.inference.auth.AuthApplier;
import com.g4vrk.react.inference.auth.AuthSettings;
import com.g4vrk.react.inference.auth.type.AuthType;
import com.g4vrk.react.inference.http.model.HttpRequest;
import com.g4vrk.react.inference.server.settings.InferenceEndpointSettings;
import com.g4vrk.react.inference.server.settings.InferenceRequestSettings;
import com.g4vrk.react.inference.server.settings.InferenceResponseSettings;
import com.g4vrk.react.inference.server.settings.InferenceSettings;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter
public final class InferenceServer {

    private static final Method HTTP_CLIENT_SHUTDOWN_NOW = findShutdownMethod();

    private final HttpClient client;
    private final AuthApplier authApplier;

    private final String baseUrl;
    private final InferenceEndpointSettings endpoint;
    private final InferenceRequestSettings requestSettings;
    private final InferenceResponseSettings responseSettings;
    private final Duration callTimeout;

    private final boolean enabled;

    public InferenceServer(
            final @NotNull Logger logger,
            final @NotNull InferenceSettings settings
    ) {
        this.client = settings.buildClient();
        this.enabled = settings.isEnabled();

        this.endpoint = settings.getEndpoint();
        this.requestSettings = settings.getRequest();
        this.responseSettings = settings.getResponse();
        this.callTimeout = settings.getCallTimeout();

        this.baseUrl = normalizeUrl(endpoint.getBaseUrl());
        this.authApplier = new AuthApplier(settings.getAuth());

        if (enabled) {
            validate(logger, settings.getAuth());
        }
    }

    public <T> @NotNull CompletableFuture<HttpResponse<T>> callAsync(
            final @NotNull HttpRequest httpRequest,
            final @NotNull HttpResponse.BodyHandler<T> bodyHandler
    ) {
        final String rawUrl = baseUrl + endpoint.getPath();
        final URI url = parseUrl(rawUrl);

        final java.net.http.HttpRequest.Builder requestBuilder =
                java.net.http.HttpRequest.newBuilder(authApplier.applyToUrl(url))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .method(
                                httpRequest.getMethod(),
                                java.net.http.HttpRequest.BodyPublishers.ofString(
                                        httpRequest.getBody(),
                                        StandardCharsets.UTF_8
                                )
                        );

        if (!callTimeout.isZero() && !callTimeout.isNegative()) {
            requestBuilder.timeout(callTimeout);
        }

        authApplier.applyToHeaders(requestBuilder);

        return client.sendAsync(requestBuilder.build(), bodyHandler);
    }

    public void augmentPayload(final @NotNull Map<String, Object> payload) {
        this.authApplier.applyToPayload(payload);
    }

    public void shutdown() {
        if (HTTP_CLIENT_SHUTDOWN_NOW == null) {
            return;
        }

        try {
            HTTP_CLIENT_SHUTDOWN_NOW.invoke(client);
        } catch (final IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Could not shut down the inference HTTP client", ex);
        }
    }

    private static Method findShutdownMethod() {
        try {
            return HttpClient.class.getMethod("shutdownNow");
        } catch (final NoSuchMethodException ignored) {
            return null;
        }
    }

    private void validate(
            final @NotNull Logger logger,
            final @NotNull AuthSettings auth
    ) {
        if (!isValidUrl(baseUrl + endpoint.getPath())) {
            logger.warn(
                    "ML inference endpoint '{}{}' is not a valid HTTP URL, requests will fail",
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

    private static @NotNull URI parseUrl(final @NotNull String rawUrl) {
        try {
            final URI uri = URI.create(rawUrl);

            if (!isHttpUrl(uri)) {
                throw new IllegalArgumentException("URL must use HTTP or HTTPS and include a host");
            }

            return uri;
        } catch (final IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid ML inference endpoint URL: '" + rawUrl + "'. " +
                            "Check inference.endpoint.base-url and inference.endpoint.path",
                    ex
            );
        }
    }

    private static boolean isValidUrl(final @NotNull String rawUrl) {
        try {
            return isHttpUrl(URI.create(rawUrl));
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isHttpUrl(final @NotNull URI uri) {
        final String scheme = uri.getScheme();

        return uri.getHost() != null
                && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
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
