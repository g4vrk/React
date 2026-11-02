package com.g4vrk.react.util.platform;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.UUID;

public final class PlayerPlatformUtil {

    private static volatile PlayerDetector floodgateDetector;
    private static volatile PlayerDetector geyserDetector;

    private PlayerPlatformUtil() {
    }

    public static boolean bedrockPlayer(final @NotNull UUID uuid) {
        final PlayerDetector floodgate = floodgateDetector();
        if (floodgate != null && floodgate.isBedrockPlayer(uuid)) {
            return true;
        }

        final PlayerDetector geyser = geyserDetector();
        return geyser != null && geyser.isBedrockPlayer(uuid);
    }

    private static PlayerDetector floodgateDetector() {
        PlayerDetector detector = floodgateDetector;

        if (detector == null) {
            synchronized (PlayerPlatformUtil.class) {
                detector = floodgateDetector;

                if (detector == null) {
                    detector = findDetector(
                            "org.geysermc.floodgate.api.FloodgateApi",
                            "getInstance",
                            "isFloodgatePlayer"
                    );

                    if (detector != null) {
                        floodgateDetector = detector;
                    }
                }
            }
        }

        return detector;
    }

    private static PlayerDetector geyserDetector() {
        PlayerDetector detector = geyserDetector;

        if (detector == null) {
            synchronized (PlayerPlatformUtil.class) {
                detector = geyserDetector;

                if (detector == null) {
                    detector = findDetector(
                            "org.geysermc.api.Geyser",
                            "api",
                            "isBedrockPlayer"
                    );

                    if (detector != null) {
                        geyserDetector = detector;
                    }
                }
            }
        }

        return detector;
    }

    private static PlayerDetector findDetector(
            final @NotNull String apiClassName,
            final @NotNull String factoryMethodName,
            final @NotNull String detectorMethodName
    ) {
        final Plugin[] plugins = Bukkit.getPluginManager().getPlugins();

        for (final Plugin plugin : plugins) {
            final PlayerDetector detector = findDetector(
                    plugin.getClass().getClassLoader(),
                    apiClassName,
                    factoryMethodName,
                    detectorMethodName
            );

            if (detector != null) {
                return detector;
            }
        }

        return findDetector(
                PlayerPlatformUtil.class.getClassLoader(),
                apiClassName,
                factoryMethodName,
                detectorMethodName
        );
    }

    private static PlayerDetector findDetector(
            final @NotNull ClassLoader classLoader,
            final @NotNull String apiClassName,
            final @NotNull String factoryMethodName,
            final @NotNull String detectorMethodName
    ) {
        try {
            final Class<?> apiClass = Class.forName(apiClassName, false, classLoader);
            final Method factory = apiClass.getMethod(factoryMethodName);
            final Object instance = factory.invoke(null);

            if (instance == null) {
                return null;
            }

            final Method detector = apiClass.getMethod(detectorMethodName, UUID.class);
            return new PlayerDetector(instance, detector);
        } catch (final ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private record PlayerDetector(
            @NotNull Object instance,
            @NotNull Method method
    ) {

        private boolean isBedrockPlayer(final @NotNull UUID uuid) {
            try {
                return Boolean.TRUE.equals(method.invoke(instance, uuid));
            } catch (final ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
    }
}
