package com.g4vrk.react.check;


import com.g4vrk.react.check.decay.DecayStrategy;
import com.g4vrk.react.check.info.CheckInfo;
import com.g4vrk.react.player.model.ReactPlayer;
import lombok.Getter;
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

    protected final void fail(double amount) {
        this.violations += amount;
    }

    protected final void reward() {
        violations = decayStrategy().decay(violations);
    }

    public abstract @NotNull DecayStrategy decayStrategy();

}