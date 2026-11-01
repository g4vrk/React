package com.g4vrk.react.api.rcp.provider;

import com.g4vrk.react.api.rcp.RCPLifecycle;
import com.g4vrk.react.api.rcp.environment.RCPEnvironment;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class RCPProvider implements RCPLifecycle {

    private final RCPEnvironment singletonEnvironment;

    public RCPProvider(
            final @NotNull RCPEnvironment environment
    ) {
        this.singletonEnvironment = Objects.requireNonNull(
                environment,
                "environment can't be null"
        );
    }

    protected @NotNull RCPEnvironment singletonEnvironment() {
        return singletonEnvironment;
    }

    @Override
    public void onEnable() {
        // pass by default
    }

    @Override
    public void onDisable() {
        // pass by default
    }
}
