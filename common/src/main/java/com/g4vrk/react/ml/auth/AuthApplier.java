package com.g4vrk.react.ml.auth;

import com.g4vrk.react.ml.auth.type.AuthType;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

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

        payload.put(field, settings.getBodyValue());
    }

    public void applyToHeaders(final @NotNull Request.Builder builder) {
        if (!isActive()) {
            return;
        }

        switch (settings.getType()) {
            case BEARER -> builder.addHeader(
                    "Authorization",
                    "Bearer " + settings.getBearerToken()
            );

            case HEADER -> {
                final String name = settings.getHeaderName();

                if (!name.isBlank()) {
                    builder.addHeader(name, settings.getHeaderValue());
                }
            }

            default -> {
            }
        }
    }

    public @NotNull HttpUrl applyToUrl(final @NotNull HttpUrl url) {
        if (!isActive() || settings.getType() != AuthType.QUERY) {
            return url;
        }

        final String parameter = settings.getQueryParameter();

        if (parameter.isBlank()) {
            return url;
        }

        return url.newBuilder()
                .addQueryParameter(parameter, settings.getQueryValue())
                .build();
    }
}
