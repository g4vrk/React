package com.g4vrk.react.paper.impl;

import com.g4vrk.react.api.ReactAPI;
import com.g4vrk.schedula.api.SchedulaAPI;
import com.g4vrk.schedula.bukkit.impl.task.runner.factory.PaperSchedulerFactory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class PaperReactAPI implements ReactAPI {

    private final SchedulaAPI api;

    public PaperReactAPI(
            @NotNull Plugin plugin
    ) {
        this.api = SchedulaAPI.builder()
                .factory(new PaperSchedulerFactory(plugin))
                .build();
    }

    @Override
    public @NotNull SchedulaAPI getSchedulaAPI() {
        return api;
    }
}
