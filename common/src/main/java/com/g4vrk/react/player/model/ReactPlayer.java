package com.g4vrk.react.player.model;

import com.g4vrk.react.React;
import com.g4vrk.react.alert.printer.AlertPrinter;
import com.g4vrk.react.check.manager.CheckManager;
import com.g4vrk.react.history.InferenceHistory;
import com.g4vrk.react.player.CombatActivity;
import com.g4vrk.react.player.model.rotation.RotationData;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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
        this.inferenceHistory = new InferenceHistory();

        this.combatActivity = new CombatActivity();
        this.rotationData = new RotationData(rotationHistorySize);
    }

}