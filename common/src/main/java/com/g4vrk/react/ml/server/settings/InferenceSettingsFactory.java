package com.g4vrk.react.ml.server.settings;

import com.g4vrk.react.ml.auth.AuthSettings;
import com.g4vrk.react.ml.auth.type.AuthType;
import com.g4vrk.react.parse.time.TimeParser;
import com.g4vrk.react.parse.time.TimeValue;
import lombok.experimental.UtilityClass;
import okhttp3.ConnectionPool;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class InferenceSettingsFactory {

    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

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
        final ConnectionPool connectionPool = parseConnectionPool(clientNode.node("connection-pool"));

        return new InferenceSettings(
                parseTimeout(clientNode, "connect-timeout"),
                parseTimeout(clientNode, "read-timeout"),
                parseTimeout(clientNode, "write-timeout"),
                parseTimeout(clientNode, "call-timeout"),
                clientNode.node("retry-on-failure").getBoolean(true),
                connectionPool,
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
        final String path = normalizePath(endpointNode.node("path").getString("/analyze"));

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
                responseNode.node("probability-field").getString("cheat_probability"),
                responseNode.node("confidence-field").getString("confidence")
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
                authNode.node("bearer", "token").getString(""),
                authNode.node("header", "name").getString("X-Subscription-Token"),
                authNode.node("header", "value").getString(""),
                authNode.node("query", "parameter").getString("token"),
                authNode.node("query", "value").getString(""),
                authNode.node("body", "field").getString("token"),
                authNode.node("body", "value").getString("")
        );
    }

    private @NotNull ConnectionPool parseConnectionPool(
            final @NotNull ConfigurationNode connectionPoolNode
    ) {
        final TimeValue keepAlive = TimeParser.parseOrDefault(
                connectionPoolNode.node("keep-alive-duration").getString("5m"),
                new TimeValue(5, TimeUnit.MINUTES)
        );

        return new ConnectionPool(
                connectionPoolNode.node("max-idle-connections").getInt(5),
                keepAlive.value(),
                keepAlive.unit()
        );
    }

    private @NotNull Duration parseTimeout(
            final @NotNull ConfigurationNode clientNode,
            final @NotNull String key
    ) {
        return TimeParser.parseDurationOrDefault(
                clientNode.node(key).getString("10s"),
                DEFAULT_TIMEOUT
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

        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }
}
