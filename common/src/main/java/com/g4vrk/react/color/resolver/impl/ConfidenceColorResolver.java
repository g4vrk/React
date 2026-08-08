package com.g4vrk.react.color.resolver.impl;

import com.g4vrk.react.color.resolver.ValueColorResolver;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public final class ConfidenceColorResolver implements ValueColorResolver {

    @Override
    public @NotNull TextColor resolve(double value) {
        value = Math.max(0.0D, Math.min(1.0D, value));

        final int red;
        final int green;

        if (value < 0.5D) {
            red = 255;
            green = (int) (value * 2.0D * 255.0D);
        } else {
            red = (int) ((1.0D - (value - 0.5D) * 2.0D) * 255.0D);
            green = 255;
        }

        return TextColor.color(red, green, 0);
    }

    @Override
    public @NotNull TextColor resolve(
            double value,
            double min,
            double max
    ) {
        value = Math.max(min, Math.min(max, value));

        final double normalized = (value - min) / (max - min);

        return resolve(normalized);
    }
}