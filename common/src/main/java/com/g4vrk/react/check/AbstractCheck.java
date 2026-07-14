package com.g4vrk.react.check;

import com.g4vrk.react.check.decay.DecayStrategy;
import com.g4vrk.react.check.decay.impl.NoDecay;
import lombok.AccessLevel;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Setter(AccessLevel.PROTECTED)
public abstract class AbstractCheck implements ReactCheck {

    private String name = "DEFAULT";
    private String configId = "default";
    private boolean experimental;

    private final DecayStrategy decayStrategy = new NoDecay();

    public AbstractCheck() {
    }

    public AbstractCheck(
            @NotNull String name,
            boolean experimental
    ) {
        this.name = name;
        this.configId = name.toLowerCase();
        this.experimental = experimental;
    }

    @Override
    public @NotNull String getName() {
        return Objects.requireNonNull(name);
    }

    @Override
    public @NotNull String getConfigId() {
        return configId;
    }

    @Override
    public boolean experimental() {
        return experimental;
    }

    @Override
    public @NotNull DecayStrategy decayStrategy() {
        return decayStrategy;
    }
}
