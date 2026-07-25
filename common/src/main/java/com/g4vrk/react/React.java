package com.g4vrk.react;

import com.g4vrk.fastTextFormatter.TextFormatter;
import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.alert.manager.AlertManager;
import com.g4vrk.react.alert.printer.AlertPrinter;
import com.g4vrk.react.alert.publish.impl.AlertPublisher;
import com.g4vrk.react.api.ReactAPI;
import com.g4vrk.react.api.task.runner.TaskRunner;
import com.g4vrk.react.api.task.runner.factory.TaskRunnerFactory;
import com.g4vrk.react.command.argument.impl.AlertsArgument;
import com.g4vrk.react.command.builder.CommandBuilderFactory;
import com.g4vrk.react.config.check.CheckConfigRegistry;
import com.g4vrk.react.config.check.impl.SimpleCheckConfigRegistry;
import com.g4vrk.react.check.processor.rotation.RotationFactory;
import com.g4vrk.react.check.processor.rotation.RotationProcessor;
import com.g4vrk.react.config.lang.Language;
import com.g4vrk.react.config.loader.UnloadedYamlConfigLoader;
import com.g4vrk.react.config.manager.YamlConfigManager;
import com.g4vrk.react.config.values.ConfigValues;
import com.g4vrk.react.listeners.bukkit.CombatListener;
import com.g4vrk.react.listeners.packet.ConnectionListener;
import com.g4vrk.react.listeners.packet.RotationListener;
import com.g4vrk.react.ml.aim.MLAimProcessor;
import com.g4vrk.react.ml.server.MLServer;
import com.g4vrk.react.player.factory.PlayerFactory;
import com.g4vrk.react.player.registry.PlayerRegistry;
import com.g4vrk.react.resource.ResourceHolder;
import com.g4vrk.react.resource.impl.PluginResourceHolder;
import com.g4vrk.react.util.PluginUtil;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
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

    public static final React INSTANCE = new React();

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
    private Config punishmentsConfig;
    private Config historyConfig;

    private ConfigValues configValues;

    private CheckConfigRegistry checkConfigRegistry;

    private TaskRunnerFactory taskRunnerFactory;

    private MLServer mlServer;
    private MLAimProcessor mlAimProcessor;

    private AlertPublisher alertPublisher;
    private AlertManager alertManager;
    private AlertPrinter alertPrinter;

    private final Set<PacketListenerCommon> registeredListeners = new ObjectOpenHashSet<>();

    public void initialize(
            final @NotNull Plugin plugin,
            final @NotNull ReactAPI api
    ) {
        this.plugin = plugin;
        this.resourceHolder = new PluginResourceHolder(plugin);
        this.taskRunnerFactory = api.getTaskRunnerFactory();
    }

    public void preLoad() {
        final LegacyPaperCommandManager<CommandSender> commandManager = LegacyPaperCommandManager.createNative(
                plugin,
                ExecutionCoordinator.simpleCoordinator()
        );

        final CommandBuilderFactory commandBuilderFactory =
                new CommandBuilderFactory(
                        commandManager,
                        "react",
                        "react.command",
                        "React anticheat main command",
                        new String[]{"reactac", "reactai", "ac"}
                );

        commandManager.command(new AlertsArgument(commandBuilderFactory, () -> alertManager).build());

        if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            try {

                commandManager.registerBrigadier();

                final CloudBrigadierManager<CommandSender, ?> cbm = commandManager.brigadierManager();

                cbm.settings().set(BrigadierSetting.FORCE_EXECUTABLE, true);

            } catch (final Throwable th) {

                logger.error("Failed to register Brigadier native completions. Falling back to standard completions.", th);

            }
        } else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {

            commandManager.registerAsynchronousCompletions();

        }
    }

    public void load() {
        if (!PluginUtil.containsPlugin("packetevents")) {
            logger.error("Plugin 'packetevents' cannot found on this server!");
            logger.error("Please, install it, otherwise React will not work.");
            logger.error("For now, disabling our plugin.");

            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        final File pluginDir = plugin.getDataFolder();

        final Language language = Language.resolve();
        final String languageNameLower = language.name().toLowerCase();

        logger.info("Language successfully resolved: {}", languageNameLower.toUpperCase());

        final File configsDir = new File(pluginDir, languageNameLower);

        final UnloadedYamlConfigLoader configLoader = new UnloadedYamlConfigLoader();

        this.yamlConfigManager = new YamlConfigManager(configLoader);

        this.yamlConfigManager.expectedConfigs(
                "checks/aim-ai.yml",
                "main-config.yml",
                "punishments.yml",
                "history.yml"
        );

        this.configMap = new Object2ObjectOpenHashMap<>();
        try {

            this.yamlConfigManager.prepareExpected(resourceHolder, languageNameLower, configsDir);

            this.configMap.putAll(yamlConfigManager.loadAndSave(configsDir));

        } catch (final Exception ex) {
            throw new RuntimeException("An internal error occurred when trying to load configurations", ex);
        }

        this.checkConfigRegistry = new SimpleCheckConfigRegistry(configMap);

        logger.info("Successfully loaded {} configurations: {}", this.configMap.size(), String.join(", ", this.configMap.keySet()));

        this.mainConfig = Objects.requireNonNull(configMap.get("main-config.yml"));
        this.punishmentsConfig = Objects.requireNonNull(configMap.get("punishments.yml"));
        this.historyConfig = Objects.requireNonNull(configMap.get("history.yml"));

        this.configValues = new ConfigValues(mainConfig.getRoot());

        final PlayerFactory playerFactory = new PlayerFactory(configValues.getBufferSize());

        this.playerRegistry = new PlayerRegistry(playerFactory);

        this.mlServer = new MLServer(configValues.getMlClientSettings());

        final TaskRunner taskRunner = taskRunnerFactory.create();

        final String alertFormatRaw = mainConfig.getRoot()
                .node("alerts", "format")
                .getString("&c«React» &7| &f{player} - {check} {verbose}");

        final Component alertFormat = textFormatter.format(alertFormatRaw);

        this.alertManager = new AlertManager();

        this.alertPublisher = new AlertPublisher(
                plugin.getServer(),
                ForkJoinPool.commonPool(),
                taskRunner,
                audience -> audience instanceof Player player
                        && player.hasPermission(Permissions.ALERTS)
                        && alertManager.receives(player.getUniqueId()),
                true
        );

        this.alertPrinter = new AlertPrinter(alertPublisher, alertFormat);

        this.mlAimProcessor = new MLAimProcessor(
                logger, mlServer, taskRunner
        );

        this.registerPacketListeners(
                new ConnectionListener(playerRegistry, playerFactory, alertPublisher),
                new RotationListener(playerRegistry, new RotationProcessor(new RotationFactory()))
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
}