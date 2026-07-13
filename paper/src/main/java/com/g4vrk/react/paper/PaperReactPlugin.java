package com.g4vrk.react.paper;

import com.g4vrk.react.React;
import com.g4vrk.react.api.ReactAPI;
import com.g4vrk.react.paper.impl.PaperReactAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperReactPlugin extends JavaPlugin {

    private final React react = React.INSTANCE;
    private ReactAPI api;

    @Override
    public void onLoad() {
        this.api = new PaperReactAPI(this);

        this.react.initialize(this, api);
    }

    @Override
    public void onEnable() {
        this.react.load();
    }

    @Override
    public void onDisable() {
        this.react.terminate();
    }

}
