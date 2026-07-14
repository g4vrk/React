package com.g4vrk.react.check.decay.impl;

import com.g4vrk.react.check.decay.DecayStrategy;

public final class ExponentialDecay implements DecayStrategy {

    private final double multiplier;

    public ExponentialDecay(
            double multiplier
    ) {
        this.multiplier = multiplier;
    }

    @Override
    public double decay(double violations) {
        return violations * multiplier;
    }

}