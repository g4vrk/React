package com.g4vrk.react.check.config.impl;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.check.Check;
import com.g4vrk.react.check.config.CheckConfigRegistry;
import com.g4vrk.react.check.info.CheckInfo;
import com.g4vrk.react.config.manager.YamlConfigManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class SimpleCheckConfigRegistry implements CheckConfigRegistry {

    private final Map<String, Config> configMap;

    public SimpleCheckConfigRegistry(
            @NotNull Map<String, Config> configMap
    ) {
        this.configMap = configMap;
    }

    @Override
    public @NotNull Config config(
            @NotNull Class<? extends Check> checkClass
    ) {

        final CheckInfo info = checkClass.getAnnotation(CheckInfo.class);

        if (info == null) {
            throw new IllegalStateException(
                    checkClass.getName() + " is missing @CheckInfo"
            );
        }

        final Config config = configMap.get(info.configId() + ".yml");

        if (config == null) {
            throw new IllegalStateException(
                    "Missing configuration for check '" + info.name() + "' (configId: " + info.configId() + ")"
            );
        }

        return config;

    }

}