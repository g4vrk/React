package com.g4vrk.react.api;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

public interface Configurable {

    void reload(@NotNull ConfigurationNode node);

}
