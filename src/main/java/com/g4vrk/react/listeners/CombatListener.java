package com.g4vrk.react.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import com.g4vrk.react.player.PlayerActivity;
import com.g4vrk.react.player.PlayerRegistry;

public class CombatListener implements Listener {

    private final long combatTicks;

    public CombatListener(
            long combatTicks
    ) {
        this.combatTicks = combatTicks;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) return;

        final PlayerActivity damagerActivity = PlayerRegistry.getActivity(damager.getUniqueId());
        final PlayerActivity victimActivity = PlayerRegistry.getActivity(victim.getUniqueId());

        if (damagerActivity != null) damagerActivity.extend(combatTicks);
        if (victimActivity != null) victimActivity.extend(combatTicks);
    }
}