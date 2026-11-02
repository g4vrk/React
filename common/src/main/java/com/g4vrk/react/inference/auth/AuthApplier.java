package com.g4vrk.react.inference.auth;

import com.g4vrk.react.inference.auth.type.AuthType;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class AuthApplier {

    private final AuthSettings settings;

    public AuthApplier(
            @NotNull AuthSettings settings
    ) {
        this.settings = settings;
    }

    public boolean isActive() {
        return settings.isEnabled() && settings.getType() != AuthType.NONE;
    }

    public void applyToPayload(
            final @NotNull Map<String, Object> payload
    ) {
        if (!isActive() || settings.getType() != AuthType.BODY) {
            return;
        }

        final String field = settings.getBodyField();

        if (field.isBlank()) {
            return;
        }

        payload.put(field, settings.getValue());
    }

    public void applyToHeaders(final @NotNull HttpRequest.Builder builder) {
        if (!isActive()) {
            return;
        }

        switch (settings.getType()) {
            case BEARER -> builder.header(
                    "Authorization",
                    "Bearer " + settings.getValue()
            );

            case HEADER -> {
                final String name = settings.getHeaderName();

                if (!name.isBlank()) {
                    builder.header(name, settings.getValue());
                }
            }

            default -> {
            }
        }
    }

    public @NotNull URI applyToUrl(final @NotNull URI url) {
        if (!isActive() || settings.getType() != AuthType.QUERY) {
            return url;
        }

        final String parameter = settings.getQueryParameter();

        if (parameter.isBlank()) {
            return url;
        }

        final String encodedParameter = URLEncoder.encode(parameter, StandardCharsets.UTF_8);
        final String encodedValue = URLEncoder.encode(settings.getValue(), StandardCharsets.UTF_8);
        final String rawUrl = url.toASCIIString();
        final int fragmentIndex = rawUrl.indexOf('#');

        final String requestUrl = fragmentIndex == -1
                ? rawUrl
                : rawUrl.substring(0, fragmentIndex);
        final String fragment = fragmentIndex == -1
                ? ""
                : rawUrl.substring(fragmentIndex);

        final String separator;

        if (!requestUrl.contains("?")) {
            separator = "?";
        } else if (requestUrl.endsWith("?") || requestUrl.endsWith("&")) {
            separator = "";
        } else {
            separator = "&";
        }

        return URI.create(
                requestUrl + separator + encodedParameter + "=" + encodedValue + fragment
        );
    }
}
