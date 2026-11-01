package com.g4vrk.react.api.rcp.internal.runtime;

import com.g4vrk.react.api.internal.runtime.impl.LifecycleRuntimeImpl;
import com.g4vrk.react.api.rcp.RCPLifecycle;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletionStage;

public final class RCPRuntime {

    private static final String THREAD_NAME = "React-RCP-Runtime";

    private final LifecycleRuntimeImpl<RCPLifecycle> runtime = new LifecycleRuntimeImpl<>(
            THREAD_NAME,
            RCPLifecycle::onEnable,
            RCPLifecycle::onDisable,
            lifecycle -> {}
    );

    public void registerProvider(
            final @NotNull RCPLifecycle provider
    ) {
        runtime.register(provider);
    }

    public @NotNull CompletionStage<Void> start() {
        return runtime.start();
    }

    public boolean running() {
        return runtime.running();
    }

    public void close() {
        runtime.close();
    }
}
