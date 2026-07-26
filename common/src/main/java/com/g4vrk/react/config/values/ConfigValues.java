package com.g4vrk.react.config.values;

import com.g4vrk.react.React;
import com.g4vrk.react.ml.server.settings.MLClientSettings;
import com.g4vrk.react.parse.time.TimeParser;
import com.g4vrk.react.parse.time.TimeValue;
import lombok.AccessLevel;
import lombok.Getter;
import okhttp3.ConnectionPool;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Getter
public final class ConfigValues {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    @Getter(AccessLevel.NONE)
    private final ConfigurationNode root;

    private MLClientSettings mlClientSettings;
    private boolean debugEnabled;
    private int bufferSize;

    public ConfigValues(
            @NotNull ConfigurationNode root
    ) {
        this.root = root;

        this.setup();
    }

    private void setup() {
        this.debugEnabled = root.node("debug").getBoolean(false);

        final ConfigurationNode bufferNode = root.node("player", "rotations-buffer-size");

        this.bufferSize = !bufferNode.virtual()
                ? bufferNode.getInt(150)
                : root.node("ml-check", "buffer-size").getInt(150);

        setupMLClientSettings(root);
    }

    private void setupMLClientSettings(final @NotNull ConfigurationNode root) {
        final ConfigurationNode mlServerNode = root.node("ml-server");
        final ConfigurationNode clientNode = mlServerNode.node("client");
        final ConfigurationNode connectionPoolNode = clientNode.node("connection-pool");

        final TimeValue timeValue = TimeParser.parseOrDefault(
                connectionPoolNode.node("keep-alive-duration").getString("5m"),
                new TimeValue(5, TimeUnit.MINUTES)
        );

        final ConnectionPool connectionPool = new ConnectionPool(
                connectionPoolNode.node("max-idle-connections").getInt(5),
                timeValue.value(),
                timeValue.unit()
        );

        this.mlClientSettings = new MLClientSettings(
                parseTimeout(clientNode, "connect-timeout"),
                parseTimeout(clientNode, "read-timeout"),
                parseTimeout(clientNode, "write-timeout"),
                parseTimeout(clientNode, "call-timeout"),
                clientNode.node("retry-on-failure").getBoolean(true),
                connectionPool,
                mlServerNode.node("enabled").getBoolean(true),
                mlServerNode.node("server-url").getString("http://localhost:8080"),
                mlServerNode.node("api-key").getString("NONE")
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
}
