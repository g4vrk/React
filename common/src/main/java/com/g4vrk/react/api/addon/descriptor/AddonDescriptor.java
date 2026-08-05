package com.g4vrk.react.api.addon.descriptor;

import com.g4vrk.react.api.addon.meta.AddonMetadata;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface AddonDescriptor<S> {

    @NotNull AddonMetadata read(@NotNull S source) throws Exception;

}
