package com.g4vrk.react.version;

import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public enum ServerVersion {

    v1_16_5(1, 16, 5),

    v1_17(1, 17, 0),
    v1_17_1(1, 17, 1),

    v1_18(1, 18, 0),
    v1_18_1(1, 18, 1),
    v1_18_2(1, 18, 2),

    v1_19(1, 19, 0),
    v1_19_1(1, 19, 1),
    v1_19_2(1, 19, 2),
    v1_19_3(1, 19, 3),
    v1_19_4(1, 19, 4),

    v1_20(1, 20, 0),
    v1_20_1(1, 20, 1),
    v1_20_2(1, 20, 2),
    v1_20_3(1, 20, 3),
    v1_20_4(1, 20, 4),
    v1_20_6(1, 20, 6),

    v1_21(1, 21, 0),
    v1_21_1(1, 21, 1),
    v1_21_2(1, 21, 2),
    v1_21_3(1, 21, 3),
    v1_21_4(1, 21, 4),
    v1_21_5(1, 21, 5),
    v1_21_6(1, 21, 6),
    v1_21_7(1, 21, 7),
    v1_21_8(1, 21, 8),
    v1_21_9(1, 21, 9),
    v1_21_11(1, 21, 11),
    v26_1(26, 1, 0),
    v26_1_1(26, 1, 1),
    v26_1_2(26, 1, 2);

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    public static final ServerVersion CURRENT;

    static {
        CURRENT = detectVersion();
    }

    private final int major;
    private final int minor;
    private final int patch;

    ServerVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public boolean isAtLeast(ServerVersion other) {
        return compareTo(other) >= 0;
    }

    public boolean isBelow(ServerVersion other) {
        return compareTo(other) < 0;
    }

    private static ServerVersion detectVersion() {
        String version = Bukkit.getBukkitVersion();

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.find()) {
            throw new IllegalStateException("Cannot detect Minecraft version: " + version);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null
                ? Integer.parseInt(matcher.group(3))
                : 0;

        ServerVersion closest = null;

        for (ServerVersion v : values()) {
            if (v.major == major && v.minor == minor) {
                if (v.patch == patch) {
                    return v;
                }
                if (v.patch <= patch) {
                    closest = v;
                }
            }
        }

        if (closest != null) {
            return closest;
        }

        throw new IllegalStateException(
                "Unsupported Minecraft version: " + major + "." + minor + "." + patch
        );
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }
}
