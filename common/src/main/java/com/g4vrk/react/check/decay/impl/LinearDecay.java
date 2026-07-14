package com.g4vrk.react.check.decay.impl;

import com.g4vrk.react.check.decay.DecayStrategy;

public final class LinearDecay implements DecayStrategy {

    private final double amount;

    public LinearDecay(
            double amount
    ) {
        this.amount = amount;
    }

    @Override
    public double decay(double violations) {
        return Math.max(0.0, violations - amount);
    }

}