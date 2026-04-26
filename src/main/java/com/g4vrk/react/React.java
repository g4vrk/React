package com.g4vrk.react;

import com.g4vrk.react.config.YamlConfig;
import com.g4vrk.react.config.lang.Language;
import com.g4vrk.react.config.manager.YamlConfigManager;
import com.g4vrk.react.config.values.ConfigValues;
import com.g4vrk.react.ml.server.MLServer;
import com.g4vrk.react.runner.factory.TaskRunnerFactory;
import com.g4vrk.react.runner.folia.factory.FoliaTaskRunnerFactory;
import com.g4vrk.react.runner.paper.factory.PaperTaskRunnerFactory;
import com.g4vrk.react.version.ServerVersion;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class React {

    public static final React INSTANCE = new React();

    public static final String NAME = "React";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static final String MAIN_CONFIG_NAME = "main-config";

    private Plugin plugin;

    private YamlConfigManager yamlConfigManager;

    private YamlConfig mainConfig;

    private ConfigValues configValues;

    private TaskRunnerFactory taskRunnerFactory;

    private MLServer mlServer;

    void initialize(final @NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        this.yamlConfigManager = new YamlConfigManager(plugin, Language.resolve());
        this.mainConfig = yamlConfigManager.getConfig(MAIN_CONFIG_NAME);

        this.configValues = new ConfigValues(mainConfig);

        this.taskRunnerFactory = ServerVersion.isFolia()
                ? new FoliaTaskRunnerFactory(plugin)
                : new PaperTaskRunnerFactory(plugin);

        this.mlServer = new MLServer(configValues.getMlClientSettings());
    }

    void terminate() {
        this.mainConfig = null;
        this.configValues = null;
        this.taskRunnerFactory = null;
        this.mlServer = null;
    }
}
