package com.g4vrk.react.inference.server.settings;

import com.g4vrk.react.inference.auth.AuthSettings;
import com.g4vrk.react.inference.auth.type.AuthType;
import com.g4vrk.react.parse.time.TimeParser;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.time.Duration;

public class InferenceSettingsFactory {

    private final Duration defaultTimeout = Duration.ofSeconds(10);

    public @NotNull InferenceSettings create(
            final @NotNull ConfigurationNode root
    ) {
        final boolean enabled = root.node("enabled").getBoolean(true);

        final ConfigurationNode inferenceNode = root.node("inference");

        final InferenceEndpointSettings endpoint = parseEndpoint(inferenceNode.node("endpoint"));
        final InferenceRequestSettings request = parseRequest(inferenceNode.node("request"));
        final InferenceResponseSettings response = parseResponse(inferenceNode.node("response"));

        final AuthSettings auth = parseAuth(root.node("auth"));

        final ConfigurationNode clientNode = root.node("client");

        return new InferenceSettings(
                parseTimeout(clientNode, "connect-timeout"),
                parseTimeout(clientNode, "call-timeout"),
                enabled,
                endpoint,
                request,
                response,
                auth
        );
    }

    private @NotNull InferenceEndpointSettings parseEndpoint(
            final @NotNull ConfigurationNode endpointNode
    ) {
        final String baseUrl = endpointNode.node("base-url").getString("http://localhost:8080");
        final String path = normalizePath(endpointNode.node("path").getString("/api/v1/inference/"));

        return new InferenceEndpointSettings(baseUrl, path);
    }

    private @NotNull InferenceRequestSettings parseRequest(
            final @NotNull ConfigurationNode requestNode
    ) {
        return new InferenceRequestSettings(
                requestNode.node("player-name-field").getString("name"),
                requestNode.node("rotations-field").getString("frames")
        );
    }

    private @NotNull InferenceResponseSettings parseResponse(
            final @NotNull ConfigurationNode responseNode
    ) {
        return new InferenceResponseSettings(
                responseNode.node("probability-field").getString("cheat_probability")
        );
    }

    private @NotNull AuthSettings parseAuth(
            final @NotNull ConfigurationNode authNode
    ) {
        final boolean enabled = authNode.node("enabled").getBoolean(false);
        final AuthType type = AuthType.safelyMatch(authNode.node("type").getString("none"), AuthType.NONE);

        return new AuthSettings(
                enabled,
                type,
                authNode.node("value").getString("null"),
                authNode.node("header", "name").getString("X-Subscription-Token"),
                authNode.node("query", "parameter").getString("token"),
                authNode.node("body", "field").getString("token")
        );
    }

    private @NotNull Duration parseTimeout(
            final @NotNull ConfigurationNode clientNode,
            final @NotNull String key
    ) {
        return TimeParser.parseDurationOrDefault(
                clientNode.node(key).getString("10s"),
                defaultTimeout
        );
    }

    private @NotNull String normalizePath(final @NotNull String rawPath) {
        String path = rawPath.trim();

        if (path.isEmpty()) {
            return "/";
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path;
    }
}
