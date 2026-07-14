package com.g4vrk.react.check.impl.aim;

import com.g4vrk.react.React;
import com.g4vrk.react.check.Check;
import com.g4vrk.react.check.decay.DecayStrategy;
import com.g4vrk.react.check.decay.impl.LinearDecay;
import com.g4vrk.react.check.info.CheckInfo;
import com.g4vrk.react.check.type.RotationCheck;
import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.game.Rotation;
import com.g4vrk.react.ml.aim.MLAimProcessor;
import com.g4vrk.react.player.model.ReactPlayer;
import com.g4vrk.react.player.model.rotation.RotationData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

@CheckInfo(
        name = "Aim-AI"
)
public final class AimAI extends Check implements RotationCheck {

    private final MLAimProcessor mlAimProcessor;
    private final ValueColorResolver colorResolver;

    private final int threshold;

    public AimAI(@NotNull ReactPlayer player) {
        super(player);
        this.mlAimProcessor = React.INSTANCE.getMlAimProcessor();
        this.threshold = 25;
        this.colorResolver = new ProbabilityColorResolver();
    }

    @Override
    public @NotNull DecayStrategy decayStrategy() {
        return new LinearDecay(0.5);
    }

    @Override
    public void onRotation(@NotNull RotationData currentData) {
        if (currentData.historySize() < threshold) return;

        final Rotation[] snapshot = currentData.snapshotHistory();

        mlAimProcessor.check(player.getName(), snapshot, this::onServerResult);
    }

    private void onServerResult(
            final double probability
    ) {

        final TextColor color = colorResolver.resolve(probability);
        final Component verbose = Component.text(probability * 100.0D + "%")
                .color(color);

        if (probability > 0.49) {
            super.failAndAlert(1, verbose);
        } else {
            super.reward();
        }

        player.rotationData.clear();

    }
    
}
