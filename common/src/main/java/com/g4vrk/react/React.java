package com.g4vrk.react;

import com.g4vrk.fastTextFormatter.TextFormatter;
import com.g4vrk.functionalActions.defaults.DefaultActions;
import com.g4vrk.functionalActions.parser.ActionParser;
import com.g4vrk.functionalActions.parser.impl.SimpleActionParser;
import com.g4vrk.functionalActions.registry.ActionRegistry;
import com.g4vrk.functionalActions.registry.impl.SimpleActionRegistry;
import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.functionalConfiguration.loader.YamlConfigLoader;
import com.g4vrk.react.alert.manager.AlertManager;
import com.g4vrk.react.alert.printer.AlertPrinter;
import com.g4vrk.react.alert.publish.impl.AlertPublisher;
import com.g4vrk.react.api.ReactAPI;
import com.g4vrk.react.api.addon.JavaAddon;
import com.g4vrk.react.api.addon.descriptor.impl.SimpleAddonDescriptor;
import com.g4vrk.react.api.addon.loader.impl.JarAddonLoader;
import com.g4vrk.react.api.addon.repository.impl.JarAddonRepository;
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
import com.g4vrk.react.ml.server.settings.InferenceSettingsFactory;
import com.g4vrk.react.ml.server.settings.InferenceSettings;
import com.g4vrk.react.parse.time.TimeParser;
import com.g4vrk.react.parse.time.TimeValue;
import com.g4vrk.react.player.factory.PlayerFactory;
import com.g4vrk.react.player.registry.PlayerRegistry;
import com.g4vrk.react.punish.PunishmentManager;
import com.g4vrk.react.resource.ResourceHolder;
import com.g4vrk.react.resource.impl.PluginResourceHolder;
import com.g4vrk.react.util.PluginUtil;
import com.g4vrk.schedula.api.SchedulaAPI;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class React {

    public static final React INSTANCE = new React();

    private static final String NAME = "React";

    private static final String[] SPECIAL_THANKS_TO = new String[]{"jvm-argument", "g4vrk"};

    private final Logger logger = LoggerFactory.getLogger(NAME);

    private final TextFormatter textFormatter = TextFormatter.textFormatter();

    private Plugin plugin;

    private Map<String, JavaAddon> addonMap;

    private ResourceHolder resourceHolder;

    private PlayerRegistry playerRegistry;

    private YamlConfigManager yamlConfigManager;

    private Map<String, Config> configMap;

    private Config actionsConfig;
    private Config mainConfig;
    private Config punishmentsConfig;
    private Config historyConfig;
    private Config inferenceConfig;

    private ConfigValues configValues;

    private CheckConfigRegistry checkConfigRegistry;

    private SchedulaAPI schedulaAPI;

    private MLServer mlServer;
    private MLAimProcessor mlAimProcessor;

    private AlertPublisher alertPublisher;
    private AlertManager alertManager;
    private AlertPrinter alertPrinter;

    private PunishmentManager punishmentManager;

    private final Set<PacketListenerCommon> registeredListeners = new ObjectOpenHashSet<>();

    private LegacyPaperCommandManager<CommandSender> commandManager;

    public void initialize(
            final @NotNull Plugin plugin,
            final @NotNull ReactAPI api
    ) {
        this.plugin = plugin;
        this.resourceHolder = new PluginResourceHolder(plugin);
        this.schedulaAPI = api.getSchedulaAPI();
    }

    public void preLoad() {
    }

    public void load() {

        logger.info("Checking for 'PacketEvents' plugin...");

        if (!PluginUtil.containsPlugin("packetevents")) {
            logger.error("Plugin 'packetevents' cannot be found on this server!");
            logger.error("Please, install it, otherwise React will not work.");
            logger.error("For now, disabling our plugin.");

            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        logger.info("Plugin 'PacketEvents' successfully found, our plugin might work normally.");

        logger.info("Creating commandManager for registering commands...");
        this.commandManager = LegacyPaperCommandManager.createNative(
                plugin,
                ExecutionCoordinator.simpleCoordinator()
        );

        logger.info("Creating commandBuilderFactory for building arguments...");
        final CommandBuilderFactory commandBuilderFactory =
                new CommandBuilderFactory(
                        commandManager,
                        "react",
                        "react.command",
                        "React anticheat main command",
                        new String[]{"reactac", "reactai", "ac"}
                );

        if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            try {

                logger.info("Registering Native Brigadier support in commandManager...");
                commandManager.registerBrigadier();

                final CloudBrigadierManager<CommandSender, ?> cbm = commandManager.brigadierManager();

                cbm.settings().set(BrigadierSetting.FORCE_EXECUTABLE, true);

            } catch (final Throwable th) {

                logger.error("Failed to register Brigadier native completions. Falling back to standard completions.", th);

            }
        } else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {

            logger.info("Registering Asynchronous completions in commandManager...");
            commandManager.registerAsynchronousCompletions();

        }

        final File pluginDir = plugin.getDataFolder();

        //noinspection ResultOfMethodCallIgnored
        pluginDir.mkdirs();

        final Language language = Language.resolve();
        final String languageNameLower = language.name().toLowerCase();

        logger.info("Language successfully resolved: {}", languageNameLower.toUpperCase());

        final File configsDir = new File(pluginDir, languageNameLower);

        final UnloadedYamlConfigLoader configLoader = new UnloadedYamlConfigLoader();

        this.yamlConfigManager = new YamlConfigManager(configLoader);

        this.yamlConfigManager.expectedConfigs(
                "checks/aim-ai.yml",
                "actions.yml",
                "main-config.yml",
                "punishments.yml",
                "history.yml",
                "inference.yml"
        );

        this.configMap = new Object2ObjectOpenHashMap<>();
        logger.info("Loading configurations...");
        try {
            logger.info("Preparing files...");
            this.yamlConfigManager.prepareExpected(resourceHolder, languageNameLower, configsDir);

            this.configMap.putAll(yamlConfigManager.loadAndSave(configsDir));
            logger.info("Configurations successfully loaded.");

        } catch (final Exception ex) {
            throw new RuntimeException("An internal error occurred when trying to load configurations", ex);
        }

        this.checkConfigRegistry = new SimpleCheckConfigRegistry(configMap);

        this.actionsConfig = Objects.requireNonNull(configMap.get("actions.yml"));
        this.mainConfig = Objects.requireNonNull(configMap.get("main-config.yml"));
        this.punishmentsConfig = Objects.requireNonNull(configMap.get("punishments.yml"));
        this.historyConfig = Objects.requireNonNull(configMap.get("history.yml"));
        this.inferenceConfig = Objects.requireNonNull(configMap.get("inference.yml"));

        logger.info("Successfully loaded {} configurations: {}", this.configMap.size(), String.join(", ", this.configMap.keySet()));

        this.configValues = new ConfigValues(mainConfig.getRoot());

        final ActionRegistry<Audience> actionRegistry = new SimpleActionRegistry<>(true);

        new DefaultActions.Adventure().registerDefaults(actionRegistry, textFormatter::format, ";");

        final ActionParser<Audience> actionParser = new SimpleActionParser<>(actionRegistry);

        logger.info("Loading all addons...");
        this.loadAddons();

        logger.info("Creating Data management modules...");
        final PlayerFactory playerFactory = new PlayerFactory(configValues.getBufferSize());

        this.playerRegistry = new PlayerRegistry(playerFactory);

        final InferenceSettings inferenceSettings = InferenceSettingsFactory.create(inferenceConfig.getRoot());

        logger.info("Creating ML server...");
        this.mlServer = new MLServer(logger, inferenceSettings);

        final Scheduler scheduler = schedulaAPI.createScheduler();

        final String alertFormatRaw = mainConfig.getRoot()
                .node("alerts", "format")
                .getString("&c«React» &7| &f{player} - {check} {verbose}");

        final Component alertFormat = textFormatter.format(alertFormatRaw);

        final boolean showAlertsInConsole = mainConfig.getRoot()
                .node("alerts", "show-in-console")
                .getBoolean(true);

        logger.info("Creating Alerts system...");
        this.alertManager = new AlertManager();

        this.alertPublisher = new AlertPublisher(
                plugin.getServer(),
                ForkJoinPool.commonPool(),
                scheduler,
                audience -> audience instanceof Player player
                        && player.hasPermission(Permissions.ALERTS)
                        && alertManager.receives(player.getUniqueId()),
                showAlertsInConsole
        );

        this.alertPrinter = new AlertPrinter(alertPublisher, alertFormat);

        this.alertPublisher.flushListeners();

        logger.info("Creating Punishment manager...");
        this.punishmentManager = new PunishmentManager(
                punishmentsConfig,
                logger,
                plugin.getServer(),
                scheduler,
                alertPrinter
        );

        this.mlAimProcessor = new MLAimProcessor(
                logger, mlServer, scheduler
        );

        logger.info("Registering command arguments...");
        commandManager.command(new AlertsArgument(
                commandBuilderFactory,
                alertPublisher,
                alertManager,
                actionParser
        ).build());

        logger.info("React main command successfully prepared!");

        logger.info("Registering packet listeners...");
        this.registerPacketListeners(
                new ConnectionListener(playerRegistry, playerFactory, alertPublisher, alertManager),
                new RotationListener(playerRegistry, new RotationProcessor(new RotationFactory()))
        );

        final long combatTicks = TimeParser.parseOrDefault(
                mainConfig.getRoot().node("player", "combat", "time").getString("8s"),
                new TimeValue(8, TimeUnit.SECONDS)
        ).toMillis() / 50L;

        final PluginManager pluginManager = plugin.getServer().getPluginManager();

        logger.info("Registering bukkit listeners...");
        pluginManager.registerEvents(
                new CombatListener(playerRegistry, combatTicks),
                plugin
        );

        logger.info("Enabling all addons...");
        this.enableAddons();

        logger.info(" ");
        logger.info("React successfully enabled!");
        logger.info(" ");
        logger.info("Thanks for using our AI powered products.");
        logger.info("Special thanks to {}", String.join(", ", SPECIAL_THANKS_TO));
        logger.info(" ");
        logger.info("Our telegram channel: https://telegram.me/react_ac");
        logger.info("Our official site: https://www.react-ac.space");
        logger.info(" ");
    }

    public void terminate() {
        logger.info("Terminating the plugin...");

        try {
            logger.info("Stopping all addons...");
            this.disableAddons();

            logger.info("Unregistering listeners...");
            this.unregisterPacketListeners();

            if (this.mlServer != null) {
                logger.info("Stopping ML server...");
                this.mlServer.shutdown();
            }

            if (this.playerRegistry != null) {
                logger.info("Clearing player registry...");
                this.playerRegistry.clear();
            }

            this.mainConfig = null;
            this.inferenceConfig = null;
            this.configValues = null;
            this.schedulaAPI = null;
            this.mlServer = null;
            this.mlAimProcessor = null;
            this.punishmentManager = null;

        } catch (final Exception ex) {
            this.logger.error("An internal error occurred when trying to terminate the plugin", ex);
        }

    }

    private void loadAddons() {

        this.addonMap = new Object2ObjectOpenHashMap<>();

        final File addonsDir = new File(this.plugin.getDataFolder(), "addons");

        //noinspection ResultOfMethodCallIgnored
        addonsDir.mkdirs();

        final Collection<Path> addonPaths = new JarAddonRepository(addonsDir).discover();

        final JarAddonLoader addonLoader = new JarAddonLoader(
                addonsDir,
                new SimpleAddonDescriptor(new YamlConfigLoader()),
                metadata -> LoggerFactory.getLogger(NAME + "#" + metadata.name())
        );

        for (final Path source : addonPaths) {
            try {

                final JavaAddon addon = addonLoader.load(source);

                addon.onLoad();

                this.addonMap.put(addon.name(), addon);

            } catch (final Exception ex) {

                //noinspection StringConcatenationArgumentToLogCall
                logger.error("Failed to load addon " + source, ex);

            }
        }
    }

    private void enableAddons() {

        for (final JavaAddon addon : this.addonMap.values()) {
            try {

                addon.setEnabled(true);

            } catch (final Exception ex) {

                //noinspection StringConcatenationArgumentToLogCall
                logger.error("Failed to enable addon " + addon.name(), ex);

            }

        }

    }

    private void disableAddons() {

        for (final JavaAddon addon : this.addonMap.values()) {
            try {

                addon.setEnabled(false);

            } catch (final Exception ex) {

                //noinspection StringConcatenationArgumentToLogCall
                logger.error("Failed to disable addon " + addon.name(), ex);

            }

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

        if (this.registeredListeners.isEmpty()) {
            return;
        }

        try {
            final EventManager eventManager = PacketEvents.getAPI().getEventManager();

            eventManager.unregisterListeners(this.registeredListeners.toArray(PacketListenerCommon[]::new));

        } finally {
            this.registeredListeners.clear();
        }

    }
}
