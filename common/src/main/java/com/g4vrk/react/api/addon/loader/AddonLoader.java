package com.g4vrk.react.api.addon.loader;

import com.g4vrk.react.api.addon.Addon;
import org.jetbrains.annotations.NotNull;

public interface AddonLoader<S> {

    @NotNull Addon load(@NotNull S source) throws Exception;

}
