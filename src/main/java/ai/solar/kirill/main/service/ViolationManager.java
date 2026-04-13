package ai.solar.kirill.main.service;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import ai.solar.kirill.SolarAI;
import ai.solar.kirill.utils.igrok.PlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ViolationManager {

    private static final double EPSILON = 0.001;

    private final SolarAI plugin;
    private final Map<UUID, Double> violationLevels = new ConcurrentHashMap<>();
    private final Map<UUID, Double> lastPunishmentThreshold = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> recentFlagTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastFlagTimestamp = new ConcurrentHashMap<>();

    private final long flagStreakWindowMillis;
    private final boolean enabled;
    private final double vlIncrement;
    private final double legacyVlIncrement;
    private final double violationThreshold;
    private final double decayAmount;
    private final boolean resetVlAfterPunishment;
    private final boolean logToConsole;
    private final String mlConsoleFormat;
    private final String checkConsoleFormat;
    private final List<PunishmentStep> punishmentSteps = new ArrayList<>();

    public ViolationManager(SolarAI plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("ml-check.enabled", true);
        this.vlIncrement = plugin.getConfig().getDouble("ml-check.vl-increment", 1.0);
        this.legacyVlIncrement = plugin.getConfig().getDouble("legacy-checks.vl-increment", this.vlIncrement);
        this.violationThreshold = plugin.getConfig().getDouble("ml-check.violation-threshold", 7.0);

        this.flagStreakWindowMillis = TimeUnit.SECONDS.toMillis(
                plugin.getConfig().getLong("mitigations.activation.flag-streak.time-window-seconds", 20)
        );

        double windowSeconds = plugin.getConfig().getDouble("ml-check.window", 600);
        if (windowSeconds > 0) {
            double checksPerWindow = windowSeconds / 60.0;
            this.decayAmount = this.vlIncrement / checksPerWindow;
        } else {
            this.decayAmount = 0;
        }

        this.resetVlAfterPunishment = plugin.getConfig().getBoolean("progressive-punishment.reset-vl-after-punishment", true);
        this.logToConsole = plugin.getConfig().getBoolean("alerts.log-to-console", true);
        this.mlConsoleFormat = plugin.getConfig().getString(
                "alerts.console.ml-format",
                "[SolarAI] ML flag: %player% prob=%prob% VL=%vl%"
        );
        this.checkConsoleFormat = plugin.getConfig().getString(
                "alerts.console.check-format",
                "[SolarAI] Check flag: %player% check=%check% VL=%vl%"
        );

        loadPunishmentSteps();

        if (enabled && this.decayAmount > 0) {
            startDecayTask();
        }
    }

    private void loadPunishmentSteps() {
        if (!plugin.getConfig().contains("progressive-punishment.steps")) {
            return;
        }

        List<Map<?, ?>> stepsList = plugin.getConfig().getMapList("progressive-punishment.steps");

        for (Map<?, ?> stepMap : stepsList) {
            try {
                Object thresholdObj = stepMap.get("threshold");
                Object commandObj = stepMap.get("command");

                if (thresholdObj == null || commandObj == null) continue;

                double threshold = thresholdObj instanceof Number
                        ? ((Number) thresholdObj).doubleValue()
                        : Double.parseDouble(thresholdObj.toString());
                String command = commandObj.toString();

                if (!command.isEmpty()) {
                    punishmentSteps.add(new PunishmentStep(threshold, command));
                }
            } catch (Exception ignored) {
            }
        }

        punishmentSteps.sort(Comparator.comparingDouble(s -> s.threshold));
    }

    public void handleViolation(PlayerEntity entity, double probability) {
        if (!enabled) return;

        UUID uuid = entity.getUuid();
        trackFlagTimestamp(uuid);
        updateLastFlagTimestamp(uuid);

        double currentVL = violationLevels.getOrDefault(uuid, 0.0);
        double newVL = currentVL + vlIncrement;
        violationLevels.put(uuid, newVL);
        if (logToConsole) {
            String msg = mlConsoleFormat
                    .replace("%player%", entity.getName())
                    .replace("%prob%", String.format("%.3f", probability))
                    .replace("%vl%", String.format("%.2f", newVL));
            Bukkit.getLogger().info(msg);
        }
        sendAlert(entity, probability, newVL);
        checkForPunishment(entity, newVL);
    }

    public void handleLegacyViolation(PlayerEntity entity, String checkName) {
        if (!enabled) return;

        UUID uuid = entity.getUuid();
        trackFlagTimestamp(uuid);
        updateLastFlagTimestamp(uuid);

        double currentVL = violationLevels.getOrDefault(uuid, 0.0);
        double newVL = currentVL + legacyVlIncrement;
        violationLevels.put(uuid, newVL);

        if (logToConsole) {
            String msg = checkConsoleFormat
                    .replace("%player%", entity.getName())
                    .replace("%check%", checkName)
                    .replace("%vl%", String.format("%.2f", newVL));
            Bukkit.getLogger().info(msg);
        }
        checkForPunishment(entity, newVL);
    }

    private void trackFlagTimestamp(UUID uuid) {
        List<Long> timestamps = recentFlagTimestamps.computeIfAbsent(uuid, k -> new LinkedList<>());
        long currentTime = System.currentTimeMillis();
        timestamps.add(currentTime);
        timestamps.removeIf(ts -> (currentTime - ts) > flagStreakWindowMillis);
    }

    private void updateLastFlagTimestamp(UUID uuid) {
        lastFlagTimestamp.put(uuid, System.currentTimeMillis());
    }

    public long getLastFlagTimestamp(UUID uuid) {
        return lastFlagTimestamp.getOrDefault(uuid, 0L);
    }

    public int getRecentFlagsCount(UUID uuid) {
        return recentFlagTimestamps.getOrDefault(uuid, Collections.emptyList()).size();
    }

    public double getViolationLevel(UUID uuid) {
        return violationLevels.getOrDefault(uuid, 0.0);
    }

    private void checkForPunishment(PlayerEntity entity, double currentVl) {
        String mode = plugin.getConfig().getString("auto-punishment.mode", "INSTANT").toUpperCase();

        switch (mode) {
            case "INSTANT":
            case "ANIMATED":
                if (currentVl >= violationThreshold) {
                    executePunishment(entity, mode);
                    violationLevels.remove(entity.getUuid());
                }
                break;
            case "PROGRESSIVE":
                executeProgressivePunishment(entity, currentVl);
                break;
            case "ALERTS_ONLY":
                break;
        }
    }

    private void executePunishment(PlayerEntity entity, String mode) {
        String commandToExecute = null;
        if ("INSTANT".equals(mode)) {
            commandToExecute = plugin.getConfig().getString("auto-punishment.command");
        } else if ("ANIMATED".equals(mode)) {
            commandToExecute = "solar punish " + entity.getName();
        }
        if (commandToExecute == null || commandToExecute.isEmpty()) return;

        String finalCommand = commandToExecute.replace("%player%", entity.getName());
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
    }

    private void executeProgressivePunishment(PlayerEntity entity, double currentVl) {
        UUID uuid = entity.getUuid();
        double lastPunishedAt = lastPunishmentThreshold.getOrDefault(uuid, 0.0);

        if (punishmentSteps.isEmpty()) return;

        PunishmentStep stepToApply = null;

        for (int i = punishmentSteps.size() - 1; i >= 0; i--) {
            PunishmentStep step = punishmentSteps.get(i);

            boolean vlReached = (currentVl + EPSILON) >= step.threshold;
            boolean notPunishedYet = (step.threshold - EPSILON) > lastPunishedAt;

            if (vlReached && notPunishedYet) {
                stepToApply = step;
                break;
            }
        }

        if (stepToApply != null) {
            String command = stepToApply.command.replace("%player%", entity.getName());

            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            );

            lastPunishmentThreshold.put(uuid, stepToApply.threshold);

            if (resetVlAfterPunishment) {
                violationLevels.put(uuid, 0.0);
                lastPunishmentThreshold.put(uuid, 0.0);
            }
        }
    }

    private void startDecayTask() {
        long decayIntervalTicks = 20L * 60;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (UUID uuid : violationLevels.keySet()) {
                double currentVL = violationLevels.get(uuid);
                double newVL = currentVL - decayAmount;

                if (newVL <= 0) {
                    violationLevels.remove(uuid);
                    lastPunishmentThreshold.remove(uuid);
                    recentFlagTimestamps.remove(uuid);
                    lastFlagTimestamp.remove(uuid);
                } else {
                    violationLevels.put(uuid, newVL);
                }
            }
        }, decayIntervalTicks, decayIntervalTicks);
    }


    private void sendAlert(PlayerEntity entity, double probability, double currentVl) {
        if (!plugin.getConfig().getBoolean("alerts.enabled", true)) return;
        String message = plugin.getLocaleManager().getMessage("alerts.message");
        if (message.isEmpty()) return;

        String formattedProb = String.format("%.2f%%", probability * 100.0D);
        String mode = plugin.getConfig().getString("auto-punishment.mode", "INSTANT").toUpperCase();
        String vlString;
        if ("PROGRESSIVE".equals(mode)) {
            vlString = String.format("%.1f", currentVl);
        } else {
            vlString = String.format("%.1f/%.0f", currentVl, violationThreshold);
        }
        String finalMessage = ChatColor.translateAlternateColorCodes('&',
                message.replace("%player%", entity.getName())
                        .replace("%probability%", formattedProb)
                        .replace("%vl%", vlString));
        String permission = plugin.getConfig().getString("alerts.permission", "solarai.alerts");
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission(permission) && plugin.areAlertsEnabledFor(admin.getUniqueId())) {
                admin.sendMessage(finalMessage);
            }
        }
    }

    public void clearPlayerData(UUID uuid) {
        violationLevels.remove(uuid);
        lastPunishmentThreshold.remove(uuid);
        recentFlagTimestamps.remove(uuid);
        lastFlagTimestamp.remove(uuid);
    }

    private static class PunishmentStep {
        private final double threshold;
        private final String command;

        public PunishmentStep(double threshold, String command) {
            this.threshold = threshold;
            this.command = command;
        }
    }
}
