package com.g4vrk.react.api.addon.internal.runtime;

import com.g4vrk.react.api.addon.JavaAddon;
import com.g4vrk.react.api.internal.runtime.LifecycleRuntime;
import com.g4vrk.react.api.internal.runtime.impl.LifecycleRuntimeImpl;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletionStage;

public final class AddonRuntime {

    private static final String THREAD_NAME = "React-Addon-Runtime";

    private final LifecycleRuntime<JavaAddon> runtime = new LifecycleRuntimeImpl<>(
            THREAD_NAME,
            addon -> addon.setEnabled(true),
            addon -> addon.setEnabled(false),
            JavaAddon::closeClassLoader
    );

    public void registerAddon(final @NotNull JavaAddon addon) {
        runtime.register(addon);
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