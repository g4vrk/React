package com.g4vrk.react.player;

import com.g4vrk.react.React;
import com.g4vrk.react.alert.printer.AlertPrinter;
import com.g4vrk.react.check.Check;
import com.g4vrk.react.check.manager.CheckManager;
import com.g4vrk.react.history.InferenceHistory;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import com.g4vrk.react.player.model.rotation.RotationData;
import com.g4vrk.react.statistic.InferenceStatistic;
import com.g4vrk.react.storage.model.PlayerStorageData;
import com.g4vrk.react.storage.model.StoredInference;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReactPlayer {

    @Getter
    private final UUID uniqueId;
    @Getter
    private final String name;

    public final Player bukkitPlayer;

    public final CombatActivity combatActivity;
    public final RotationData rotationData;

    public final AlertPrinter alertPrinter;
    public final CheckManager checkManager;

    public final InferenceHistory inferenceHistory;
    public final InferenceStatistic inferenceStatistic;

    private final AtomicBoolean dataReady = new AtomicBoolean();
    private final long sessionStartedAt = System.currentTimeMillis();

    public ReactPlayer(
            @NotNull UUID uniqueId,
            @NotNull String name,
            @NotNull Player bukkitPlayer,
            int rotationHistorySize
    ) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.bukkitPlayer = bukkitPlayer;
        this.alertPrinter = React.INSTANCE.getAlertPrinter();
        this.checkManager = new CheckManager(this);
        this.inferenceHistory = new InferenceHistory(
                React.INSTANCE.getStorageManager().historyRetention(),
                entry -> React.INSTANCE.getStorageManager().saveInference(
                        uniqueId,
                        new StoredInference(
                                entry.getTimestamp(),
                                entry.getCheck().getConfigId(),
                                entry.getProbability(),
                                entry.getConfidence()
                        )
                )
        );
        this.inferenceStatistic = new InferenceStatistic(this.inferenceHistory);

        this.combatActivity = new CombatActivity();
        this.rotationData = new RotationData(rotationHistorySize);
    }

    public boolean isDataReady() {
        return dataReady.get();
    }

    public void markDataReady() {
        dataReady.set(true);
    }

    public void applyStorageData(final @NotNull PlayerStorageData data) {
        applyViolations(data, false);
        inferenceHistory.replace(mapHistory(data));
    }

    public void mergeStorageData(final @NotNull PlayerStorageData data) {
        applyViolations(data, true);
        inferenceHistory.merge(mapHistory(data));
    }

    private void applyViolations(final @NotNull PlayerStorageData data, final boolean merge) {
        data.violations().forEach((configId, stored) -> {
            final Check check = checkManager.getCheck(configId);
            if (check != null) {
                final double violations;
                if (!merge) {
                    violations = stored.value();
                } else if (stored.updatedAt() >= sessionStartedAt) {
                    violations = check.getViolations();
                } else {
                    violations = check.getViolations() + stored.value();
                }
                check.restoreViolations(violations);
            }
        });
    }

    private @NotNull List<InferenceHistoryEntry> mapHistory(final @NotNull PlayerStorageData data) {
        final List<InferenceHistoryEntry> loadedHistory = new ArrayList<>(data.inferenceHistory().size());
        for (final StoredInference stored : data.inferenceHistory()) {
            final Check check = checkManager.getCheck(stored.check());
            if (check != null) {
                loadedHistory.add(new InferenceHistoryEntry(
                        stored.timestamp(),
                        check,
                        stored.probability(),
                        stored.confidence()
                ));
            }
        }
        return loadedHistory;
    }

}
