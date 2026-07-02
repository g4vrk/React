package com.g4vrk.react.color.resolver;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public interface ValueColorResolver {

    @NotNull TextColor resolve(double value);

    @NotNull TextColor resolve(double value, double min, double max);

}