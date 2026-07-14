package com.g4vrk.react.check.config;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.check.Check;
import org.jetbrains.annotations.NotNull;

public interface CheckConfigRegistry {

    @NotNull Config config(@NotNull Class<? extends Check> checkClass);

}