package com.g4vrk.react.listeners.bukkit;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.React;
import com.g4vrk.react.api.ReloadObserver;
import com.g4vrk.react.parse.time.TimeParser;
import com.g4vrk.react.parse.time.TimeValue;
import com.g4vrk.react.player.ReactPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import com.g4vrk.react.player.CombatActivity;
import com.g4vrk.react.player.registry.PlayerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class CombatListener implements Listener, ReloadObserver {

    private final PlayerRegistry playerRegistry;

    private long combatTicks;

    public CombatListener(
            @NotNull PlayerRegistry playerRegistry
    ) {
        this.playerRegistry = playerRegistry;

        this.reload();
    }

    public void reload() {

        final Config config = React.INSTANCE.getMainConfig();

        this.onReload(config);

    }

    @Override
    public void onReload(@NotNull Config config) {

        combatTicks = TimeParser.parseOrDefault(
                config.node("player", "combat", "time").getString("2s"),
                new TimeValue(8, TimeUnit.SECONDS)
        ).toMillis() / 50L;

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player)) return;

        final ReactPlayer localDamager = playerRegistry.getPlayer(damager.getUniqueId());

        final CombatActivity damagerCombatActivity = localDamager != null ? localDamager.combatActivity : null;

        if (damagerCombatActivity != null) damagerCombatActivity.extend(combatTicks);
    }
}
