package com.g4vrk.react.listeners.bukkit;

import com.g4vrk.react.player.model.ReactPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import com.g4vrk.react.player.CombatActivity;
import com.g4vrk.react.player.registry.PlayerRegistry;
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

        final ReactPlayer localDamager = playerRegistry.getPlayer(damager.getUniqueId());
        final ReactPlayer localVictim = playerRegistry.getPlayer(victim.getUniqueId());

        final CombatActivity damagerCombatActivity = localDamager != null ? localDamager.combatActivity : null;
        final CombatActivity victimCombatActivity = localVictim != null ? localVictim.combatActivity : null;

        if (damagerCombatActivity != null) damagerCombatActivity.extend(combatTicks);
        if (victimCombatActivity != null) victimCombatActivity.extend(combatTicks);
    }
}