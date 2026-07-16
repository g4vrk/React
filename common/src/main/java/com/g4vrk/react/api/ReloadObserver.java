package com.g4vrk.react.api;

import com.g4vrk.functionalConfiguration.Config;
import org.jetbrains.annotations.NotNull;

public interface ReloadObserver {

    void onReload(@NotNull Config config);

}
