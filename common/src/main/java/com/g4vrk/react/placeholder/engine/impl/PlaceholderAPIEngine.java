package com.g4vrk.react.placeholder.engine.impl;

import com.g4vrk.react.placeholder.Closure;
import com.g4vrk.react.placeholder.engine.AbstractPlaceholderEngine;
import com.g4vrk.react.util.placeholder.PlaceholderAPIUtil;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class PlaceholderAPIEngine extends AbstractPlaceholderEngine {

    public PlaceholderAPIEngine(
            @NotNull Closure closure
    ) {
        super(closure);
    }

    @Override
    protected @NotNull String resolve(
            final @NotNull OfflinePlayer player,
            final @NotNull String placeholder
    ) {
        final String original = closure().applyTo(placeholder);

        final String parsed = PlaceholderAPIUtil.setPlaceholders(
                player,
                "%" + placeholder + "%"
        );

        return parsed.equals("%" + placeholder + "%")
                ? original
                : parsed;
    }
}