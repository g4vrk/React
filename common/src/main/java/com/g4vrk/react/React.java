package com.g4vrk.react;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.alert.manager.AlertManager;
import com.g4vrk.react.alert.printer.AlertPrinter;
import com.g4vrk.react.alert.publish.impl.AlertPublisher;
import com.g4vrk.react.api.ReactAPI;
import com.g4vrk.react.api.task.runner.TaskRunner;
import com.g4vrk.react.api.task.runner.factory.TaskRunnerFactory;
import com.g4vrk.react.config.lang.Language;
import com.g4vrk.react.config.loader.YamlUnloadedConfigLoader;
import com.g4vrk.react.config.manager.YamlConfigManager;
import com.g4vrk.react.config.values.ConfigValues;
import com.g4vrk.react.listeners.bukkit.CombatListener;
import com.g4vrk.react.listeners.packet.ConnectionListener;
import com.g4vrk.react.listeners.packet.RotationListener;
import com.g4vrk.react.ml.check.MLCheck;
import com.g4vrk.react.ml.check.processor.MLCheckProcessor;
import com.g4vrk.react.ml.server.MLServer;
import com.g4vrk.react.player.PlayerRegistry;
import com.g4vrk.react.resource.ResourceHolder;
import com.g4vrk.react.resource.impl.PluginResourceHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class React {

    public static final React INSTANCE = new React();

    public static final String NAME = "React";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private Plugin plugin;
    private ResourceHolder resourceHolder;

    private PlayerRegistry playerRegistry;

    private YamlConfigManager yamlConfigManager;

    private Map<String, Config> configMap;
    private Config mainConfig;

    private ConfigValues configValues;

    private TaskRunnerFactory taskRunnerFactory;

    private MLServer mlServer;

    private AlertPublisher alertPublisher;
    private AlertManager alertManager;

    public void initialize(
            final @NotNull Plugin plugin,
            final @NotNull ReactAPI api
    ) {
        this.plugin = plugin;
        this.resourceHolder = new PluginResourceHolder(plugin);
        this.taskRunnerFactory = api.getTaskRunnerFactory();
    }

    public void load() {
        final File pluginDir = plugin.getDataFolder();

        final Language language = Language.resolve();
        final String languageName = language.name();
        LOGGER.info("Language successfully resolved: {}", languageName);

        final File configsDir = new File(pluginDir, languageName.toLowerCase());

        final YamlUnloadedConfigLoader configLoader = new YamlUnloadedConfigLoader();

        this.yamlConfigManager = new YamlConfigManager(configLoader);

        this.yamlConfigManager.expectedConfigs(
                "main-config.yml"
        );

        this.configMap = new Object2ObjectOpenHashMap<>();
        try {
            this.yamlConfigManager.prepareExpected(resourceHolder, languageName, configsDir);

            this.configMap.putAll(yamlConfigManager.loadAndSave(resourceHolder, configsDir));
        } catch (final Exception ex) {
            throw new RuntimeException("An internal error occurred when trying to load configurations", ex);
        }

        LOGGER.info("Successfully loaded {} configurations: \n{}", this.configMap.size(), String.join(", ", this.configMap.keySet()));

        this.mainConfig = Objects.requireNonNull(configMap.get("main-config.yml"));

        this.configValues = new ConfigValues(mainConfig.getRoot());

        this.playerRegistry = new PlayerRegistry(configValues.getBufferSize());

        this.mlServer = new MLServer(configValues.getMlClientSettings());

        final TaskRunner taskRunner = taskRunnerFactory.create();

        final String alertFormatRaw = mainConfig.getRoot()
                .node("alerts", "format")
                .getString("&c«React» &7| &f{player} - {check} {verbose}");
        final Component alertFormat = LegacyComponentSerializer.legacyAmpersand().deserialize(alertFormatRaw);

        final MLCheck mlCheck = createMlCheck(taskRunner, alertFormat);

        final EventManager eventManager = PacketEvents.getAPI().getEventManager();

        eventManager.registerListener(
                new ConnectionListener(playerRegistry, alertPublisher, configValues.getBufferSize())
        );
        eventManager.registerListener(
                new RotationListener(playerRegistry, mlCheck)
        );

        final long combatTicks = 20L * Math.max(1,
                mainConfig.getRoot().node("ml-check", "combat-seconds").getInt(8));

        final PluginManager pluginManager = plugin.getServer().getPluginManager();

        pluginManager.registerEvents(
                new CombatListener(playerRegistry, combatTicks),
                plugin
        );

        LOGGER.info("React успешно включен.");
    }

    private @NonNull MLCheck createMlCheck(@NotNull TaskRunner taskRunner, @NotNull Component alertFormat) {
        this.alertManager = new AlertManager();

        this.alertPublisher = new AlertPublisher(
                plugin.getServer(),
                ForkJoinPool.commonPool(),
                taskRunner,
                audience -> audience instanceof Player player
                        && player.hasPermission("react.alerts")
                        && alertManager.receives(player.getUniqueId()),
                true
        );

        final AlertPrinter alertPrinter = new AlertPrinter(alertPublisher, alertFormat);

        final MLCheckProcessor mlCheckProcessor = new MLCheckProcessor(
                LOGGER, alertPrinter, mlServer, taskRunner
        );

        return new MLCheck(
                taskRunner, mlCheckProcessor, 10
        );
    }

    public void terminate() {
        this.mainConfig = null;
        this.configValues = null;
        this.taskRunnerFactory = null;
        this.mlServer = null;
    }
    
}