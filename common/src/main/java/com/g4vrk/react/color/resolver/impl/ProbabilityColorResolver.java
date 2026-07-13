package com.g4vrk.react.color.resolver.impl;

import com.g4vrk.react.color.resolver.ValueColorResolver;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public final class ProbabilityColorResolver implements ValueColorResolver {

    @Override
    public @NotNull TextColor resolve(double value) {
        return resolve(value, 0.0, 1.0);
    }

    @Override
    public @NotNull TextColor resolve(double value, double min, double max) {
        value = Math.min(max, Math.max(value, min));

        double normalized = (value - min) / (max - min);

        int red;
        int green;

        if (normalized < 0.5D) {
            red = (int) (normalized * 2.0D * 255);
            green = 255;
        } else {
            red = 255;
            green = (int) ((1.0D - (normalized - 0.5D) * 2.0D) * 255);
        }

        return TextColor.color(red, green, 0);
    }

}