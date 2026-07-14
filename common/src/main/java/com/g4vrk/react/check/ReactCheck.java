package com.g4vrk.react.check;

import com.g4vrk.react.check.decay.DecayStrategy;
import org.jetbrains.annotations.NotNull;

public interface ReactCheck {

    @NotNull String getName();

    @NotNull String getConfigId();

    boolean experimental();

    @NotNull DecayStrategy decayStrategy();

}
