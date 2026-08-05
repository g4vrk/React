package com.g4vrk.react.folia.impl;

import com.g4vrk.react.api.ReactAPI;
import com.g4vrk.schedula.api.SchedulaAPI;
import com.g4vrk.schedula.folia.impl.task.runner.factory.FoliaSchedulerFactory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class FoliaReactAPI implements ReactAPI {

    private final SchedulaAPI api;

    public FoliaReactAPI(
            @NotNull Plugin plugin
    ) {
        this.api = SchedulaAPI.builder()
                .factory(new FoliaSchedulerFactory(plugin))
                .build();
    }

    @Override
    public @NotNull SchedulaAPI getSchedulaAPI() {
        return api;
    }
}
