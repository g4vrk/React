package com.g4vrk.react.config.values;

import com.g4vrk.react.config.YamlConfig;
import com.g4vrk.react.ml.server.settings.MLClientSettings;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class ConfigValues {
    @Getter(AccessLevel.NONE) private final YamlConfig mainConfig;

    private MLClientSettings mlClientSettings;

    public ConfigValues(
            final @NotNull YamlConfig mainConfig
    ) {
        this.mainConfig = mainConfig;
    }

    private void setup() {
    }
}
