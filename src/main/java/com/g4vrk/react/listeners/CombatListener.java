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

    public CombatListener(long combatTicks) {
        this.combatTicks = combatTicks;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) return;

        PlayerActivity a1 = PlayerRegistry.getActivity(damager.getUniqueId());
        PlayerActivity a2 = PlayerRegistry.getActivity(victim.getUniqueId());

        if (a1 != null) a1.extend(combatTicks);
        if (a2 != null) a2.extend(combatTicks);
    }
}