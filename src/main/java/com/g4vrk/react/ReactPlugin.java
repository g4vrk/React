package com.g4vrk.react;

import org.bukkit.plugin.java.JavaPlugin;

public class ReactPlugin extends JavaPlugin {

    private final React react = React.INSTANCE;

    public ReactPlugin() {
        react.initialize(this);
    }

    @Override
    public void onEnable() {
        react.load();
    }

    @Override
    public void onDisable() {

    }
}
