package com.g4vrk.reactdataset;

import com.g4vrk.functionalActions.parser.ActionParser;
import com.g4vrk.config.Config;
import com.g4vrk.config.loader.YamlConfigLoader;
import com.g4vrk.react.React;
import com.g4vrk.react.api.Factory;
import com.g4vrk.react.api.addon.JavaAddon;
import com.g4vrk.react.check.processor.rotation.RotationFactory;
import com.g4vrk.reactdataset.action.CollectorActionService;
import com.g4vrk.reactdataset.api.DatasetCollectorAPI;
import com.g4vrk.reactdataset.collector.DatasetCollector;
import com.g4vrk.reactdataset.command.argument.CollectorHelpArgument;
import com.g4vrk.reactdataset.command.argument.CollectorReloadArgument;
import com.g4vrk.reactdataset.command.argument.RecordStartArgument;
import com.g4vrk.reactdataset.command.argument.RecordStopArgument;
import com.g4vrk.reactdataset.command.argument.StatusArgument;
import com.g4vrk.reactdataset.command.argument.UploadArgument;
import com.g4vrk.reactdataset.config.DatasetSettings;
import com.g4vrk.reactdataset.config.DatasetSettingsFactory;
import com.g4vrk.reactdataset.display.RecordingBossBarService;
import com.g4vrk.reactdataset.listeners.bukkit.RecordingConnectionListener;
import com.g4vrk.reactdataset.listeners.packet.RotationListener;
import com.g4vrk.reactdataset.model.RecordLabel;
import com.g4vrk.reactdataset.session.manager.SessionManager;
import com.g4vrk.reactdataset.storage.CsvDatasetStorage;
import com.g4vrk.reactdataset.upload.UploadClient;
import com.g4vrk.reactdataset.upload.UploadQueue;
import com.g4vrk.schedula.task.Task;
import com.g4vrk.schedula.task.TickSchedule;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.incendo.cloud.Command;
import org.slf4j.Logger;

public final class ReactDatasetAddon
extends JavaAddon
implements DatasetCollectorAPI {
    public static ReactDatasetAddon INSTANCE;
    private Config mainConfig;
    private DatasetSettings settings;
    private CollectorActionService actions;
    private Scheduler scheduler;
    private DatasetCollector collector;
    private UploadQueue uploadQueue;
    private RotationListener rotationListener;
    private AutoCloseable universeSubscription;
    private RecordingBossBarService recordingBossBars;
    private RecordingConnectionListener connectionListener;
    private final List<Task> tasks = new ArrayList<Task>();

    public void onLoad() {
        INSTANCE = this;
    }

    public void onEnable() {
        Path configPath = this.directory().toPath().resolve("main-config.yml");
        if (!configPath.toFile().exists()) {
            this.saveResource("main-config.yml", false);
        }
        this.loadConfiguration();
        this.actions = new CollectorActionService((ActionParser<Audience>)React.INSTANCE.getActionParser());
        this.actions.reload(this.mainConfig);
        this.startRuntime();
        this.registerCommands();
        this.slf4jLogger().info("ReactDatasetCollector enabled. Dataset directory: {}", (Object)this.collector.datasetsDirectory().toAbsolutePath());
    }

    public void onDisable() {
        this.stopRuntime();
        this.slf4jLogger().info("ReactDatasetCollector disabled.");
    }

    public synchronized void reloadAddon() {
        Config previousConfig = this.mainConfig;
        DatasetSettings previousSettings = this.settings;
        try {
            this.loadConfiguration();
            this.actions.reload(this.mainConfig);
        }
        catch (Throwable throwable) {
            this.mainConfig = previousConfig;
            this.settings = previousSettings;
            if (this.actions != null && previousConfig != null) {
                this.actions.reload(previousConfig);
            }
            throw throwable;
        }
        this.stopRuntime();
        this.startRuntime();
    }

    private void loadConfiguration() {
        Path path = this.directory().toPath().resolve("main-config.yml");
        try {
            Config loaded = new YamlConfigLoader().from(path);
            DatasetSettings loadedSettings = new DatasetSettingsFactory().create(loaded);
            this.mainConfig = loaded;
            this.settings = loadedSettings;
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to load main-config.yml", ex);
        }
    }

    private void startRuntime() {
        React react = React.INSTANCE;
        this.scheduler = react.getSchedulaAPI().createScheduler();
        Path datasetsDir = this.settings.resolveDatasetsDirectory(react.getPlugin().getDataFolder().toPath());
        CsvDatasetStorage storage = new CsvDatasetStorage(this.slf4jLogger(), datasetsDir, this.settings.collector().framesPerFile());
        SessionManager sessions = new SessionManager(storage);
        this.collector = new DatasetCollector(this.slf4jLogger(), react.getPlayerRegistry(), new RotationFactory(), sessions, storage, this.scheduler, this.settings.collector().combatOnly(), this.settings.collector().maxFilesPerRecording());
        if (this.settings.upload().enabled()) {
            UploadClient client = new UploadClient(this.settings.upload().baseUrl(), this.settings.upload().token(), this.settings.upload().timeoutSeconds());
            this.uploadQueue = new UploadQueue(this.slf4jLogger(), storage, client, this.settings.upload().afterUpload());
            this.collector.onFileWritten(this.uploadQueue::processAsync);
            this.tasks.add(this.scheduler.scheduleAsync(this.uploadQueue::processPending, TickSchedule.instant()));
            this.tasks.add(this.scheduler.scheduleAsync(this.uploadQueue::processPending, TickSchedule.fixedRate((long)(this.settings.upload().retrySeconds() * 20L))));
        } else {
            this.uploadQueue = null;
        }
        this.recordingBossBars = new RecordingBossBarService(this.scheduler, this.settings.bossBar(), this.settings.collector().framesPerFile());
        final RecordingBossBarService activeBossBars = this.recordingBossBars;
        this.collector.onProgress((uuid, snapshot) -> activeBossBars.update(snapshot));
        this.collector.onEnd(activeBossBars::hide);
        this.connectionListener = new RecordingConnectionListener(this, this.recordingBossBars);
        Bukkit.getPluginManager().registerEvents((Listener)this.connectionListener, react.getPlugin());
        final DatasetCollector activeCollector = this.collector;
        this.universeSubscription = react.getUniverseService().subscribe(
                activeCollector::isUniverseRecording, activeCollector::onUniverseFrame);
        this.rotationListener = new RotationListener(react.getPlayerRegistry(), this.collector);
        PacketEvents.getAPI().getEventManager().registerListeners(new PacketListenerCommon[]{this.rotationListener});
    }

    private void stopRuntime() {
        if (universeSubscription != null) {
            try {
                universeSubscription.close();
            } catch (Exception failure) {
                slf4jLogger().warn("Could not unsubscribe dataset collector", failure);
            }
            universeSubscription = null;
        }
        if (this.connectionListener != null) {
            HandlerList.unregisterAll((Listener)this.connectionListener);
            this.connectionListener = null;
        }
        if (this.recordingBossBars != null) {
            this.recordingBossBars.clear();
            this.recordingBossBars = null;
        }
        if (this.rotationListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListeners(new PacketListenerCommon[]{this.rotationListener});
            this.rotationListener = null;
        }
        for (Task task : this.tasks) {
            if (task == null || task.isCancelled()) continue;
            task.cancel();
        }
        this.tasks.clear();
        if (this.collector != null) {
            this.collector.stopAll();
        }
        this.collector = null;
        this.uploadQueue = null;
        this.scheduler = null;
    }

    private void registerCommands() {
        React react = React.INSTANCE;
        String[] aliases = (String[])this.settings.command().aliases().toArray(String[]::new);
        Factory<Command.Builder<CommandSender>> builderFactory = () -> react.getCommandBuilderFactory().create().literal(this.settings.command().literal(), aliases);
        react.getCommandManager().command(new CollectorHelpArgument(builderFactory, this).build());
        react.getCommandManager().command(new RecordStartArgument(builderFactory, this, RecordLabel.SOFT).build());
        react.getCommandManager().command(new RecordStartArgument(builderFactory, this, RecordLabel.LEGIT).build());
        for (RecordLabel label : RecordLabel.values()) {
            RecordStartArgument start = new RecordStartArgument(builderFactory, this, label);
            react.getCommandManager().command(start.buildNew(false));
            react.getCommandManager().command(start.buildNew(true));
        }
        react.getCommandManager().command(new RecordStopArgument(builderFactory, this).build());
        react.getCommandManager().command(new StatusArgument(builderFactory, this, false).build());
        react.getCommandManager().command(new StatusArgument(builderFactory, this, true).build());
        react.getCommandManager().command(new UploadArgument(builderFactory, this).build());
        react.getCommandManager().command(new CollectorReloadArgument(builderFactory, this).build());
    }

    public Config mainConfig() {
        return this.mainConfig;
    }

    public DatasetSettings settings() {
        return this.settings;
    }

    public CollectorActionService actions() {
        return this.actions;
    }

    public UploadQueue uploadQueue() {
        return this.uploadQueue;
    }

    public RecordingBossBarService recordingBossBars() {
        return this.recordingBossBars;
    }

    public Logger logger() {
        return this.slf4jLogger();
    }

    @Override
    public DatasetCollector collector() {
        return this.collector;
    }
}
