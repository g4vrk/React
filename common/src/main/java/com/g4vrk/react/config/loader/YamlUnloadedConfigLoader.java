package com.g4vrk.react.config.loader;

import com.g4vrk.functionalConfiguration.YamlConfig;
import com.g4vrk.functionalConfiguration.loader.AbstractConfigLoader;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;

public class YamlUnloadedConfigLoader extends AbstractConfigLoader<YamlConfig> {

    @Override
    protected @NonNull YamlConfig from(@NotNull YamlConfigurationLoader yamlConfigurationLoader) throws IOException {
        // in the functionalConfiguration lib, the loaded configuration is loaded immediately
        // but here, we don't load it
        return new YamlConfig(yamlConfigurationLoader);
    }

}
