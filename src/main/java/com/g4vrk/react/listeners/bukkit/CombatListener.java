package com.g4vrk.react.listeners.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import com.g4vrk.react.player.CombatActivity;
import com.g4vrk.react.player.PlayerRegistry;
import org.jetbrains.annotations.NotNull;

public class CombatListener implements Listener {

    private final PlayerRegistry playerRegistry;

    private final long combatTicks;

    public CombatListener(
            @NotNull PlayerRegistry playerRegistry,
            long combatTicks
    ) {
        this.playerRegistry = playerRegistry;
        this.combatTicks = combatTicks;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) return;

        final CombatActivity damagerCombatActivity = playerRegistry.getActivity(damager.getUniqueId());
        final CombatActivity victimCombatActivity = playerRegistry.getActivity(victim.getUniqueId());

        if (damagerCombatActivity != null) damagerCombatActivity.extend(combatTicks);
        if (victimCombatActivity != null) victimCombatActivity.extend(combatTicks);
    }
}