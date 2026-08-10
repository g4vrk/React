package com.g4vrk.react.api.event;

import com.g4vrk.react.check.Check;
import com.g4vrk.react.player.ReactPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PlayerCheckFailEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ReactPlayer player;
    private final Check check;

    public PlayerCheckFailEvent(
            @NotNull ReactPlayer player,
            @NotNull Check check
    ) {
        this.player = player;
        this.check = check;
    }

    public @NotNull ReactPlayer player() {
        return player;
    }

    public @NotNull Check check() {
        return check;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}