package com.g4vrk.react.check.impl.aim;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.check.Check;
import com.g4vrk.react.check.decay.DecayStrategy;
import com.g4vrk.react.check.decay.impl.LinearDecay;
import com.g4vrk.react.check.info.CheckInfo;
import com.g4vrk.react.check.type.RotationCheck;
import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.player.model.rotation.Rotation;
import com.g4vrk.react.ml.aim.MLAimProcessor;
import com.g4vrk.react.player.model.ReactPlayer;
import com.g4vrk.react.player.model.rotation.RotationData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

@CheckInfo(
        name = "Aim-AI",
        configId = "aim-ai"
)
public final class AimAI extends Check implements RotationCheck {

    private final MLAimProcessor mlAimProcessor;
    private final ValueColorResolver colorResolver;

    private final int requiredSamples;
    private final double alertThreshold;

    private final DecayStrategy decayStrategy;

    public AimAI(@NotNull ReactPlayer player) {
        super(player);
        this.mlAimProcessor = React.INSTANCE.getMlAimProcessor();
        this.colorResolver = new ProbabilityColorResolver();

        final Config config = React.INSTANCE.getCheckConfigRegistry()
                .config(getClass());

        this.requiredSamples = config.node("required-samples").getInt(25);
        this.alertThreshold = config.node("alert", "threshold").getDouble(0.49D);

        final double decayAmount = config.node("decay", "amount").getDouble(0.5D);

        this.decayStrategy = new LinearDecay(decayAmount);
    }

    @Override
    public @NotNull DecayStrategy decayStrategy() {
        return this.decayStrategy;
    }

    @Override
    public void onRotation(@NotNull RotationData currentData) {
        if (!player.combatActivity.isActive() || currentData.historySize() < this.requiredSamples) return;

        final Rotation[] snapshot = currentData.snapshotHistory();

        this.mlAimProcessor.check(this.player.getName(), snapshot, this::onServerResult);
    }

    private void onServerResult(
            final double probability
    ) {

        final TextColor color = this.colorResolver.resolve(probability);
        final Component verbose = Component.text(probability * 100.0D + "%")
                .color(color);

        if (probability > this.alertThreshold) {
            super.failAndAlert(1, verbose);
        } else {
            super.reward();
        }

        player.rotationData.clear();

    }

}
