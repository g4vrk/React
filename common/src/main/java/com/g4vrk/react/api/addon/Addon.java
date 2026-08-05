package com.g4vrk.react.api.addon;

import com.g4vrk.react.api.addon.meta.AddonMetadata;
import com.g4vrk.react.resource.ResourceHolder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;

public interface Addon extends ResourceHolder {

    @NotNull File directory();

    @NotNull String name();
    @NotNull AddonMetadata metadata();

    @NotNull Logger slf4jLogger();

    boolean enabled();

    void onLoad();
    void onEnable();
    void onDisable();

}
