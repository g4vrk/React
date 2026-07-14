package com.g4vrk.react.check;


import com.g4vrk.react.check.decay.DecayStrategy;
import com.g4vrk.react.check.info.CheckInfo;
import com.g4vrk.react.player.model.ReactPlayer;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public abstract class Check extends AbstractCheck {

    protected final ReactPlayer player;

    @Getter
    private double violations;

    protected Check(
            @NotNull ReactPlayer player
    ) {
        super();

        final CheckInfo info = this.getClass().getAnnotation(CheckInfo.class);

        super.setName(info.name());
        super.setExperimental(info.experimental());

        this.player = player;
    }

    protected final void fail(
            final double amount
    ) {
        this.violations += amount;
    }

    protected final void failAndAlert(
            final double amount
    ) {
        this.failAndAlert(amount, Component.empty());
    }

    protected final void failAndAlert(double amount, Component verbose) {
        this.fail(amount);
        this.player.alertPrinter.print(player, getName(), verbose);
    }

    protected final void reward() {
        violations = decayStrategy().decay(violations);
    }

    public abstract @NotNull DecayStrategy decayStrategy();

}