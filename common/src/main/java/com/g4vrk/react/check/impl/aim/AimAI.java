package com.g4vrk.react.check.impl.aim;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.api.event.InferenceHistoryEntryAddedEvent;
import com.g4vrk.react.check.Check;
import com.g4vrk.react.check.debug.DebugHandler;
import com.g4vrk.react.check.decay.DecayStrategy;
import com.g4vrk.react.check.decay.impl.LinearDecay;
import com.g4vrk.react.check.info.CheckInfo;
import com.g4vrk.react.check.type.RotationCheck;
import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.history.entry.InferenceHistoryEntry;
import com.g4vrk.react.ml.aim.MLAimProcessor;
import com.g4vrk.react.ml.aim.MLResult;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.model.rotation.Rotation;
import com.g4vrk.react.player.model.rotation.RotationData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

@CheckInfo(
        name = "Aim-AI",
        configId = "aim-ai"
)
public final class AimAI extends Check implements RotationCheck, ReloadObserver {

    private final ValueColorResolver colorResolver;

    private final DebugHandler debugHandler;

    private MLAimProcessor mlAimProcessor;

    private boolean debug;

    private int requiredSamples;
    private double alertThreshold;

    private final AtomicBoolean requesting = new AtomicBoolean();

    private DecayStrategy decayStrategy;

    public AimAI(@NotNull ReactPlayer player) {
        super(player);

        this.mlAimProcessor = React.INSTANCE.getMlAimProcessor();
        this.colorResolver = new ProbabilityColorResolver();

        this.debugHandler = new DebugHandler(this);
    }

    @Override
    public void onReload(@NotNull Config config) {

        this.debug = config.node("debug").getBoolean();
        this.requiredSamples = Math.max(3, config.node("required-samples").getInt(25));
        this.alertThreshold = config.node("alert", "threshold").getDouble(0.49D);

        final double decayAmount = config.node("decay", "amount").getDouble(0.5D);

        this.decayStrategy = new LinearDecay(decayAmount);

        this.mlAimProcessor = React.INSTANCE.getMlAimProcessor();

    }

    @Override
    public @NotNull DecayStrategy decayStrategy() {
        return this.decayStrategy;
    }

    @Override
    public void onRotation(@NotNull RotationData currentData) {

        if (!player.combatActivity.isActive()
                || !shouldCheck()
                || !requesting.compareAndSet(false, true)) {
            return;
        }

        final int sampleWindowSize = Math.min(
                requiredSamples,
                currentData.historyCapacity()
        );

        final Rotation[] snapshot = currentData.drainHistory(sampleWindowSize);

        if (snapshot == null) {
            requesting.set(false);
            return;
        }

        if (debug) {
            debugHandler.debug("Sending ML request (" + snapshot.length + " rotations)");
        }

        try {
            mlAimProcessor.check(
                    player.getName(),
                    snapshot,
                    this::onServerResult
            );
        } catch (final Throwable th) {
            requesting.set(false);
            React.INSTANCE.getLogger().warn(
                    "Could not start ML request for {}",
                    player.getName(),
                    th
            );
        }
    }

    private void onServerResult(
            final @NotNull MLResult result
    ) {
        try {

            if (!player.bukkitPlayer.isOnline() || !shouldCheck()) {
                return;
            }

            if (!result.isAvailable()) {

                if (debug) {
                    debugHandler.debug("ML result unavailable");
                }

                return;
            }

            final double probability = result.getProbability();
            final double confidence = result.getConfidence();

            if (debug) {
                debugHandler.debug(
                        result.hasConfidence()
                                ? "Received ML response: " + probability + " (confidence: " + confidence + ")"
                                : "Received ML response: " + probability
                );
            }

            final TextColor color = colorResolver.resolve(probability);

            final Component verbose = Component.text(
                    probability
            ).color(color);

            if (probability > alertThreshold) {

                if (debug) {
                    debugHandler.debug(
                            "Flagged: " + probability + " > " + alertThreshold
                    );
                }

                failAndAlert(1, verbose);

            } else {

                if (debug) {
                    debugHandler.debug(
                            "Reward: " + probability + " <= " + alertThreshold
                    );
                }

                reward();
            }

            final InferenceHistoryEntry entry = new InferenceHistoryEntry(this, probability, confidence);

            player.inferenceHistory.add(entry);

            new InferenceHistoryEntryAddedEvent(player, entry).callEvent();

        } finally {

            requesting.set(false);

            if (debug) {
                debugHandler.debug("Request finished");
            }
        }
    }

}
