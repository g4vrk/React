package com.g4vrk.react.universe;

import com.g4vrk.react.React;
import com.g4vrk.react.check.impl.aim.AimAI;
import com.g4vrk.react.inference.InferenceModel;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** One UNIVERSE stream per player, shared by inference and addon subscribers. */
public final class UniverseService {
    private final PlayerRegistry players;
    private final UniverseEntityRunner entityRunner;
    private final Map<UUID, UniverseData> playerData = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    public UniverseService(PlayerRegistry players, Scheduler scheduler) {
        this.players = players;
        this.entityRunner = new UniverseEntityRunner(React.INSTANCE.getPlugin(), scheduler);
    }

    public UniverseData claimPlayerData(UUID uuid) {
        // Keep one object through the Netty/Bukkit join handoff; packets can
        // arrive while ReactPlayer is being constructed but not yet published.
        return playerData.computeIfAbsent(uuid, ignored -> new UniverseData());
    }

    public void removePlayerData(UUID uuid) {
        UniverseData removed = playerData.remove(uuid);
        if (removed != null) {
            removed.reset();
        }
    }

    public void onPacketBeforeJoin(UUID uuid, Player bukkitPlayer, PacketReceiveEvent event) {
        onPacket(uuid, bukkitPlayer, claimPlayerData(uuid), event);
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
            // Recording start/stop must not reset the continuous UNIVERSE history.
            player.universeData.clearHistory();
        }
    }

    public void onPacket(ReactPlayer player, PacketReceiveEvent event) {
        onPacket(player.getUniqueId(), player.bukkitPlayer, player.universeData, event);
    }

    private void onPacket(UUID uuid, Player bukkitPlayer, UniverseData data, PacketReceiveEvent event) {
        // Packet flags, target selection and the combat deadline must move together.
        // Capture uses this same monitor before resolving the target and reading UNIVERSE.
        synchronized (data) {
            processPacket(uuid, bukkitPlayer, data, event);
        }
    }

    private void processPacket(UUID uuid, Player bukkitPlayer, UniverseData data, PacketReceiveEvent event) {
        // UNIVERSE updates features on every rotation from join, including before a
        // recording and outside combat. Only writing frames is combat-gated.
        UniverseState state = data.activate();
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            state.onSwing();
        } else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            state.onSlotChange();
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                // UNIVERSE resolves attacks only against indexed players; unknown targets remain null.
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
            if (!flying.hasRotationChanged() || bukkitPlayer == null) {
                return;
            }
            float yaw = flying.getLocation().getYaw();
            float pitch = flying.getLocation().getPitch();
            entityRunner.run(bukkitPlayer, () -> capture(uuid, bukkitPlayer, data, state, yaw, pitch));
        }
    }

    private void capture(UUID uuid, Player bukkitPlayer, UniverseData data,
                         UniverseState state, float yaw, float pitch) {
        ReactPlayer player;
        UniverseFrame frame;
        synchronized (data) {
            player = players.getPlayer(uuid);
            if (!bukkitPlayer.isOnline()
                    || (player == null ? playerData.get(uuid) != data : player.universeData != data)
                    || !data.isCurrent(state)) {
                return;
            }
            UUID targetId = state.targetId();
            ReactPlayer targetPlayer = targetId == null ? null : players.getPlayer(targetId);
            Entity target = data.combatActivity.isActive() && targetPlayer != null
                    ? targetPlayer.bukkitPlayer : null;
            frame = state.capture(bukkitPlayer, target, yaw, pitch);
            if (player == null) {
                return;
            }
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
        }
        // Dataset checkpoints and UI callbacks must not hold up packet state updates.
        if (!data.isCurrent(state)) {
            return;
        }
        for (Subscription subscription : subscriptions) {
            if (subscription.interested.test(player)) {
                subscription.consumer.accept(player, frame);
            }
        }
    }

    private record Subscription(Predicate<ReactPlayer> interested,
                                BiConsumer<ReactPlayer, UniverseFrame> consumer) {
    }
}
