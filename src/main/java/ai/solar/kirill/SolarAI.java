package ai.solar.kirill;

import com.github.retrooper.packetevents.PacketEvents;
import com.squareup.moshi.Moshi;
import okhttp3.OkHttpClient;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ai.solar.kirill.main.comand.CommandManager;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import ai.solar.kirill.main.listeners.ConnectionListener;
import ai.solar.kirill.main.listeners.PacketListener;
import ai.solar.kirill.utils.file.LocaleManager;
import ai.solar.kirill.main.service.ViolationManager;
import ai.solar.kirill.main.service.DataCollectionService;
import ai.solar.kirill.utils.file.Config;
import ai.solar.kirill.main.database.ViolationDatabase;
import ai.solar.kirill.main.menu.MenuManager;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class SolarAI extends JavaPlugin {
    private static SolarAI instance;
    private static boolean isServerConnected = false;
    private int heartbeatTaskID = -1;
    private String serverUrl = null;
    private final Set<UUID> alertsDisabledAdmins = ConcurrentHashMap.newKeySet();
    private OkHttpClient httpClient;
    private ViolationManager violationManager;
    private LocaleManager localeManager;
    private ViolationDatabase violationDatabase;
    private MenuManager menuManager;
    private DataCollectionService dataCollectionService;
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.serverUrl = getConfig().getString("server.url", "http://localhost:8000");

        this.localeManager = new LocaleManager(this);
        getLogger().info("SolarAI вклучаитца...");
        Config.init(this);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.violationDatabase = new ViolationDatabase(this);
        this.violationManager = new ViolationManager(this);
        this.menuManager = new MenuManager(this);
        this.dataCollectionService = new DataCollectionService(this);
        isServerConnected = true;
        initializePluginServices();
        getLogger().info("SolarAI вклучен.");

        CommandManager commandManager = new CommandManager(this);
        if (getCommand("solar") != null) {
            getCommand("solar").setExecutor(commandManager);
            getCommand("solar").setTabCompleter(commandManager);
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.getConsoleSender().sendMessage("[SolarAI] Plugin enabled.");
        }, 20L);
    }


    private void initializePluginServices() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false).reEncodeByDefault(false);
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager().registerListeners(new ConnectionListener(), new PacketListener());
        PacketEvents.getAPI().init();
        long interval = 20L * 60 * 5;
        if (getConfig().getBoolean("database.auto-cleanup", true)) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                int keepDays = getConfig().getInt("database.keep-days", 7);
                long olderThan = keepDays * 24L * 60 * 60 * 1000;
                violationDatabase.cleanOldViolations(olderThan);
            }, 20L * 60 * 60, 20L * 60 * 60 * 24);
        }
    }
    public LocaleManager getLocaleManager() {
        return this.localeManager;
    }
    @Override
    public void onDisable() {
        if (heartbeatTaskID != -1) {
            Bukkit.getScheduler().cancelTask(heartbeatTaskID);
        }
        if (menuManager != null) {
            menuManager.closeAllMenus();
        }
        if (dataCollectionService != null) {
            dataCollectionService.shutdown();
        }
        if (violationDatabase != null) {
            violationDatabase.close();
        }
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
        try {
            if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isLoaded()) {
                PacketEvents.getAPI().terminate();
            }
        } catch (NoClassDefFoundError ignored) {
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        localeManager.loadMessages();
    }

    public boolean toggleAlerts(UUID uuid) {
        if (alertsDisabledAdmins.contains(uuid)) {
            alertsDisabledAdmins.remove(uuid);
            return true;
        } else {
            alertsDisabledAdmins.add(uuid);
            return false;
        }
    }

    public boolean areAlertsEnabledFor(UUID uuid) {
        return !alertsDisabledAdmins.contains(uuid);
    }

    public static SolarAI getInstance() {
        return instance;
    }

    public static boolean isServerActive() {
        return isServerConnected;
    }

    public String getServerUrl() {
        return this.serverUrl;
    }


    public OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    public ViolationManager getViolationManager() {
        return this.violationManager;
    }

    public ViolationDatabase getViolationDatabase() {
        return this.violationDatabase;
    }

    public MenuManager getMenuManager() {
        return this.menuManager;
    }

    public DataCollectionService getDataCollectionService() {
        return this.dataCollectionService;
    }

    private static class LazyHolder {
        private static final Moshi MOSHI = new Moshi.Builder().build();
        private static final Type MAP_STRING_STRING_TYPE = Types.newParameterizedType(Map.class, String.class, String.class);
        private static final JsonAdapter<Map<String, String>> JSON_ADAPTER = MOSHI.adapter(MAP_STRING_STRING_TYPE);
        private static final Type MAP_STRING_OBJECT_TYPE = Types.newParameterizedType(Map.class, String.class, Object.class);
        private static final JsonAdapter<Map<String, Object>> RESPONSE_ADAPTER = MOSHI.adapter(MAP_STRING_OBJECT_TYPE);
    }
}
