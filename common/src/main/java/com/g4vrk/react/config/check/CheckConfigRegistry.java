package com.g4vrk.react.config.check;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.check.Check;
import com.g4vrk.react.check.ReactCheck;
import org.jetbrains.annotations.NotNull;

public interface CheckConfigRegistry {

    @NotNull Config config(@NotNull Class<? extends ReactCheck> checkClass);

}