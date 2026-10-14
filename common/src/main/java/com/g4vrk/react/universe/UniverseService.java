package com.g4vrk.react.universe;

import com.g4vrk.react.React;
import com.g4vrk.react.check.impl.aim.AimAI;
import com.g4vrk.react.inference.InferenceModel;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import com.g4vrk.schedula.task.TickSchedule;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.bukkit.entity.Entity;
import java.util.UUID;

/** One SHARD stream per player, shared by inference and addon subscribers. */
public final class UniverseService {
    private final PlayerRegistry players;
    private final Scheduler scheduler;
    private final CopyOnWriteArrayList<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    public UniverseService(PlayerRegistry players, Scheduler scheduler) {
        this.players = players;
        this.scheduler = scheduler;
    }

    public AutoCloseable subscribe(Predicate<ReactPlayer> interested,
                                   BiConsumer<ReactPlayer, UniverseFrame> consumer) {
        Subscription subscription = new Subscription(interested, consumer);
        subscriptions.add(subscription);
        return () -> {
            subscriptions.remove(subscription);
            for (ReactPlayer player : players.all()) {
                refresh(player.getUniqueId());
            }
        };
    }

    public void refresh(UUID uuid) {
        ReactPlayer player = players.getPlayer(uuid);
        if (player != null && React.INSTANCE.getInferenceServer().getModel() != InferenceModel.UNIVERSE) {
            // Recording start/stop must not reset the continuous SHARD history.
            player.universeData.clearHistory();
        }
    }

    public void onPacket(ReactPlayer player, PacketReceiveEvent event) {
        UniverseData data = player.universeData;
        // Raveon updates SHARD on every rotation from join, including before a
        // recording and outside combat. Only writing frames is combat-gated.
        UniverseState state = data.activate();
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            state.onSwing();
        } else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            state.onSlotChange();
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                // Raveon resolves attacks only against indexed players; unknown targets remain null.
                ReactPlayer target = players.getByEntityId(interact.getEntityId());
                state.onAttack(target == null ? null : target.getUniqueId());
                if (!data.combatActivity.isActive()) {
                    data.clearHistory();
                }
                data.combatActivity.extend(Math.max(1L,
                        React.INSTANCE.getInferenceConfig().node("universe", "combat-ticks").getLong(64L)));
            }
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            state.onFlying(flying.hasRotationChanged());
            if (!flying.hasRotationChanged()) {
                return;
            }
            float yaw = flying.getLocation().getYaw();
            float pitch = flying.getLocation().getPitch();
            scheduler.scheduleEntity(player.bukkitPlayer, () -> capture(player, state, yaw, pitch),
                    TickSchedule.instant());
        }
    }

    private void capture(ReactPlayer player, UniverseState state, float yaw, float pitch) {
        UniverseData data = player.universeData;
        synchronized (data) {
            if (!player.bukkitPlayer.isOnline() || players.getPlayer(player.getUniqueId()) != player
                    || !data.isCurrent(state)) {
                return;
            }
            ReactPlayer targetPlayer = state.targetId() == null ? null : players.getPlayer(state.targetId());
            Entity target = data.combatActivity.isActive() && targetPlayer != null
                    ? targetPlayer.bukkitPlayer : null;
            UniverseFrame frame = state.capture(player.bukkitPlayer, target, yaw, pitch);
            if (React.INSTANCE.getInferenceServer().getModel() == InferenceModel.UNIVERSE) {
                if (data.combatActivity.isActive()) {
                    data.push(frame);
                    AimAI check = (AimAI) player.checkManager.getRotationCheck(AimAI.class);
                    if (check != null) {
                        check.onUniverseFrame();
                    }
                } else {
                    data.clearHistory();
                }
            }
            for (Subscription subscription : subscriptions) {
                if (subscription.interested.test(player)) {
                    subscription.consumer.accept(player, frame);
                }
            }
        }
    }

    private record Subscription(Predicate<ReactPlayer> interested,
                                BiConsumer<ReactPlayer, UniverseFrame> consumer) {
    }
}
