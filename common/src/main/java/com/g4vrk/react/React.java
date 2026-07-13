package com.g4vrk.react;

import com.g4vrk.fastTextFormatter.TextFormatter;
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
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class React {

    private static final React INSTANCE = new React();

    private static final String NAME = "React";

    private static final String[] SPECIAL_THANKS_TO = new String[]{"jvm-argument", "g4vrk"};

    private final Logger logger = LoggerFactory.getLogger(NAME);

    private final TextFormatter textFormatter = TextFormatter.textFormatter();

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

    private final Set<PacketListenerCommon> registeredListeners = new ObjectOpenHashSet<>();

    public static @NotNull React getSingletonInstance() {
        return INSTANCE;
    }

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
        logger.info("Language successfully resolved: {}", languageName);

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

        logger.info("Successfully loaded {} configurations: \n{}", this.configMap.size(), String.join(", ", this.configMap.keySet()));

        this.mainConfig = Objects.requireNonNull(configMap.get("main-config.yml"));

        this.configValues = new ConfigValues(mainConfig.getRoot());

        this.playerRegistry = new PlayerRegistry(configValues.getBufferSize());

        this.mlServer = new MLServer(configValues.getMlClientSettings());

        final TaskRunner taskRunner = taskRunnerFactory.create();

        final String alertFormatRaw = mainConfig.getRoot()
                .node("alerts", "format")
                .getString("&c«React» &7| &f{player} - {check} {verbose}");

        final Component alertFormat = textFormatter.format(alertFormatRaw);

        final MLCheck mlCheck = createMlCheck(taskRunner, alertFormat);

        final EventManager eventManager = PacketEvents.getAPI().getEventManager();

        final ConnectionListener connectionListener = new ConnectionListener(playerRegistry, alertPublisher, configValues.getBufferSize());


        eventManager.registerListener(
                connectionListener
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

        logger.info(" ");
        logger.info("React successfully enabled!");
        logger.info(" ");
        logger.info("Thanks for using our AI powered products.");
        logger.info("Special thanks to {}", String.join(", ", SPECIAL_THANKS_TO));
        logger.info(" ");
        logger.info("Our telegram channel: https://telegram.me/react_ac");
        logger.info(" ");
    }

    public void terminate() {

        try {

            this.unregisterPacketListeners();

            this.mainConfig = null;
            this.configValues = null;
            this.taskRunnerFactory = null;
            this.mlServer = null;

        } catch (final Exception ex) {
            this.logger.error("An internal error occurred when trying to terminate the plugin", ex);
        }

    }

    private void registerPacketListeners(
            final @NotNull PacketListenerCommon @NotNull ... packetListeners
    ) {

        final EventManager eventManager = PacketEvents.getAPI().getEventManager();

        eventManager.registerListeners(packetListeners);

        this.registeredListeners.addAll(new ObjectArrayList<>(packetListeners));

    }

    private void unregisterPacketListeners() {

        final EventManager eventManager = PacketEvents.getAPI().getEventManager();

        eventManager.unregisterListeners(this.registeredListeners.toArray(PacketListenerCommon[]::new));

    }

    private @NotNull MLCheck createMlCheck(@NotNull TaskRunner taskRunner, @NotNull Component alertFormat) {
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
                logger, alertPrinter, mlServer, taskRunner
        );

        return new MLCheck(
                taskRunner, mlCheckProcessor, 10
        );
    }

}