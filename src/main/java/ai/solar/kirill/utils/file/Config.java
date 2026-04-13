package ai.solar.kirill.utils.file;


import org.bukkit.configuration.file.FileConfiguration;
import ai.solar.kirill.SolarAI;

public class Config {
    private static SolarAI plugin;

    public static void init(SolarAI pl) {
        plugin = pl;
    }

    private static SolarAI getPlugin() {
        if (plugin == null) {
            plugin = SolarAI.getInstance();
        }
        return plugin;
    }

    private static String wrapPath(String path) {
        return "legacy-checks." + path;
    }

    private static FileConfiguration cfg() {
        return getPlugin().getConfig();
    }

    public static String getString(String path, String def) {
        String full = wrapPath(path);
        String str = cfg().getString(full);
        return str != null ? str : def;
    }

    public static boolean getBoolean(String path, boolean def) {
        String full = wrapPath(path);
        if (cfg().contains(full)) {
            return cfg().getBoolean(full);
        }
        return def;
    }

    public static int getInt(String path, int def) {
        String full = wrapPath(path);
        if (cfg().contains(full)) {
            return cfg().getInt(full);
        }
        return def;
    }

    public static double getDouble(String path, double def) {
        String full = wrapPath(path);
        if (cfg().contains(full)) {
            return cfg().getDouble(full);
        }
        return def;
    }
}

