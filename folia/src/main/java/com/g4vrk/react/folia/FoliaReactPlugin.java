package com.g4vrk.react.folia;

import com.g4vrk.react.React;
import com.g4vrk.react.api.ReactAPI;
import com.g4vrk.react.folia.impl.FoliaReactAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class FoliaReactPlugin extends JavaPlugin {

    private final React react = React.INSTANCE;

    @Override
    public void onLoad() {
        final ReactAPI api = new FoliaReactAPI(this);

        this.react.initialize(this, api);
        this.react.preLoad();
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
