package com.g4vrk.react;

import com.g4vrk.fastTextFormatter.TextFormatter;
import com.g4vrk.react.alert.printer.AlertPrinter;
import com.g4vrk.react.alert.publish.impl.AlertPublisher;
import com.g4vrk.react.config.YamlConfig;
import com.g4vrk.react.config.lang.Language;
import com.g4vrk.react.config.manager.YamlConfigManager;
import com.g4vrk.react.config.values.ConfigValues;
import com.g4vrk.react.listeners.bukkit.CombatListener;
import com.g4vrk.react.listeners.packet.ConnectionListener;
import com.g4vrk.react.listeners.packet.RotationListener;
import com.g4vrk.react.ml.check.MLCheck;
import com.g4vrk.react.ml.check.processor.MLCheckProcessor;
import com.g4vrk.react.ml.server.MLServer;
import com.g4vrk.react.player.PlayerRegistry;
import com.g4vrk.react.runner.TaskRunner;
import com.g4vrk.react.runner.factory.TaskRunnerFactory;
import com.g4vrk.react.runner.paper.factory.PaperTaskRunnerFactory;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class React {

    public static final React INSTANCE = new React();

    public static final String NAME = "React";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static final String MAIN_CONFIG_NAME = "main-config";

    private Plugin plugin;

    private PlayerRegistry playerRegistry;

    private YamlConfigManager yamlConfigManager;

    private YamlConfig mainConfig;

    private ConfigValues configValues;

    private TaskRunnerFactory taskRunnerFactory;

    private MLServer mlServer;
    private AlertPublisher alertPublisher;

    void initialize(final @NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        this.yamlConfigManager = new YamlConfigManager(plugin, Language.resolve());
        this.mainConfig = yamlConfigManager.getConfig(MAIN_CONFIG_NAME);

        this.configValues = new ConfigValues(mainConfig);

        this.playerRegistry = new PlayerRegistry(configValues.getBufferSize());

        this.taskRunnerFactory = new PaperTaskRunnerFactory(plugin);

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

//        final PluginCommand reactCommand = ((JavaPlugin) plugin).getCommand("react");
//        if (reactCommand != null) {
//            final ReactCommand executor = new ReactCommand(mutedAlerts);
//            reactCommand.setExecutor(executor);
//            reactCommand.setTabCompleter(executor);
//        } else {
//            LOGGER.warn("Команда 'react' не объявлена в plugin.yml — /react работать не будет.");
//        }

        LOGGER.info("React включён: url={}", mlServer.getServerUrl());
    }

    private @NonNull MLCheck createMlCheck(@NotNull TaskRunner taskRunner, @NotNull Component alertFormat) {
        final Set<UUID> mutedAlerts = ConcurrentHashMap.newKeySet();

        this.alertPublisher = new AlertPublisher(
                plugin.getServer(),
                ForkJoinPool.commonPool(),
                taskRunner,
                audience -> audience instanceof Player player
                        && player.hasPermission("react.alerts")
                        && !mutedAlerts.contains(player.getUniqueId()),
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

    void terminate() {
        this.mainConfig = null;
        this.configValues = null;
        this.taskRunnerFactory = null;
        this.mlServer = null;
    }
}