package com.g4vrk.react.config.values;

import com.g4vrk.functionalConfiguration.YamlConfig;
import com.g4vrk.react.ml.server.settings.MLClientSettings;
import com.g4vrk.react.parse.duration.DurationParser;
import com.g4vrk.react.parse.time.TimeUnitParser;
import com.g4vrk.react.parse.time.TimeValue;
import lombok.AccessLevel;
import lombok.Getter;
import okhttp3.ConnectionPool;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

@Getter
public final class ConfigValues {
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
        final ConfigurationNode mlCheckNode = root.node("ml-check");
        this.bufferSize = mlCheckNode.node("buffer-size").getInt(150);

        setupMLClientSettings(root);
    }

    private void setupMLClientSettings(final @NotNull ConfigurationNode root) {
        final ConfigurationNode mlServerNode = root.node("ml-server");
        final ConfigurationNode clientNode = mlServerNode.node("client");
        final ConfigurationNode connectionPoolNode = clientNode.node("connection-pool");

        final TimeValue timeValue = TimeUnitParser.parse(
                connectionPoolNode.node("keep-alive-duration")
                        .getString("5m")
        );

        final ConnectionPool connectionPool = new ConnectionPool(
                connectionPoolNode.node("max-idle-connections").getInt(5),
                timeValue.value(),
                timeValue.unit()
        );

        this.mlClientSettings = new MLClientSettings(
                DurationParser.parse(clientNode.node("connect-timeout").getString("10s")),
                DurationParser.parse(clientNode.node("read-timeout").getString("10s")),
                DurationParser.parse(clientNode.node("write-timeout").getString("10s")),
                DurationParser.parse(clientNode.node("call-timeout").getString("10s")),
                clientNode.node("retry-on-failure").getBoolean(true),
                connectionPool,
                mlServerNode.node("enabled").getBoolean(true),
                mlServerNode.node("server-url").getString("localhost:8080"),
                mlServerNode.node("api-key").getString("NONE")
        );
    }
}
