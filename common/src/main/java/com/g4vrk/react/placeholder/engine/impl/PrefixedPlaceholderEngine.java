package com.g4vrk.react.placeholder.engine.impl;

import com.g4vrk.react.placeholder.Closure;
import com.g4vrk.react.placeholder.engine.AbstractPlaceholderEngine;
import com.g4vrk.react.placeholder.provider.prefixed.PrefixedPlaceholderProvider;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class PrefixedPlaceholderEngine extends AbstractPlaceholderEngine {

    private final Map<String, PrefixedPlaceholderProvider> providers = new Object2ObjectOpenHashMap<>();

    public PrefixedPlaceholderEngine(
            @NotNull Closure closure,
            @NotNull Map<String, PrefixedPlaceholderProvider> providers
    ) {
        super(closure);
        this.providers.putAll(providers);
    }

    public @NotNull PrefixedPlaceholderEngine add(
            final @NotNull PrefixedPlaceholderProvider provider
    ) {
        this.providers.put(provider.prefix(), provider);
        return this;
    }

    @Override
    protected @NotNull String resolve(
            @NotNull OfflinePlayer player,
            @NotNull String placeholder
    ) {
        final int separator = placeholder.indexOf('.');

        if (separator == -1) {
            return placeholder;
        }

        final String prefix = placeholder.substring(0, separator);
        final String value = placeholder.substring(separator + 1);

        final PrefixedPlaceholderProvider provider = this.providers.get(prefix);

        if (provider == null) {
            return placeholder;
        }

        return provider.provide(player, value);
    }
}