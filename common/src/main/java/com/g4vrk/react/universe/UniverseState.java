package com.g4vrk.react.universe;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

// SHARD calculations ported from the supplied Raveon 1.0.3-v2.
public final class UniverseState {
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private float lastDeltaYaw = 0.0f;
    private float lastDeltaPitch = 0.0f;
    private float lastAccelYaw = 0.0f;
    private float lastAccelPitch = 0.0f;
    private float lastYawToTargetDiff = 0.0f;
    private float lastPitchToTargetDiff = 0.0f;
    private static final double SHARD_MS_PER_TICK = 50.0;
    private static final int SHARD_MAX_TICKS = 255;
    private static final float SHARD_TICKS_PER_SECOND = 20.0f;
    private static final double SHARD_RAYTRACE_RANGE_MARGIN = 3.0;
    private static final double SHARD_RAYTRACE_MIN_RANGE = 6.0;
    private static final double SHARD_DEFAULT_INTERACTION_RANGE = 3.0;
    private static final double SHARD_TARGETS_IN_RANGE_RADIUS = 6.0;
    private static final double SHARD_TARGETS_IN_RANGE_RADIUS_SQ = 36.0;
    private static final double[] SHARD_POSSIBLE_EYE_HEIGHTS = new double[]{1.62, 1.27, 0.4};
    private static final double SHARD_STAND_EYE_HEIGHT = 1.62;
    private static final double SHARD_SNEAK_EYE_HEIGHT = 1.27;
    private static final double SHARD_SWIM_EYE_HEIGHT = 0.4;
    private static final double SHARD_MIN_AIM_DISTANCE = 0.001;
    private static final double SHARD_MIN_ANGULAR_EXTENT = 1.0E-6;
    private static final float SHARD_DEFAULT_ATTACK_SPEED = 4.0f;
    private static final int SHARD_ANCHOR_TIMEOUT_TICKS = 100;
    private static final float SHARD_NO_TICKS_TO_ATTACK = -255.0f;
    private static final int SHARD_SIGNIFICANT_SAMPLES_THRESHOLD = 15;
    private static final int SHARD_TOTAL_SAMPLES_THRESHOLD = 80;
    private static final double SHARD_MAX_SAMPLE_DELTA = 5.0;
    private static final double SHARD_YAW_ULP_DIVISOR_FRACTION = 0.0625;
    private static final double SHARD_YAW_BUCKET_ULP_FACTOR = 2.0;
    private static final double SHARD_MODE_THRESHOLD = 0.001;
    private static final double SHARD_MINIMUM_DIVISOR = (double)((float)Math.pow(0.2f, 3.0)) * 8.0 * 0.15 - 0.001;
    private static final int ITEM_UNKNOWN = 0;
    private static final int ITEM_EMPTY = 1;
    private static final int ITEM_MELEE_WEAPON = 2;
    private static final int ITEM_END_CRYSTAL = 3;
    private static final int ITEM_CRYSTAL_BASE = 4;
    private static final int ITEM_RESPAWN_ANCHOR = 5;
    private static final int ITEM_GLOWSTONE = 6;
    private static final int ITEM_TOTEM = 7;
    private static final int ITEM_PLACEABLE_BLOCK = 8;
    private static final int ITEM_RANGED = 9;
    private static final int ITEM_PROJECTILE = 10;
    private static final int ITEM_CONSUMABLE = 11;
    private static final int ITEM_SHIELD = 12;
    private static final int MEMORY_SHORT = 200;
    private static final int MEMORY_LONG = 600;
    private static final Attribute ATTACK_SPEED_ATTRIBUTE = UniverseState.resolveAttribute("GENERIC_ATTACK_SPEED", "ATTACK_SPEED");
    private static final Attribute INTERACTION_RANGE_ATTRIBUTE = UniverseState.resolveAttribute("PLAYER_ENTITY_INTERACTION_RANGE", "ENTITY_INTERACTION_RANGE");
    private static volatile Boolean modernCombatServer;
    private final Object shardLock = new Object();
    private final ArrayDeque<Double> xRotModeList = new ArrayDeque<>(80);
    private final Map<Double, Integer> xRotModePopularity = new HashMap<Double, Integer>();
    private final ArrayDeque<Double> yRotModeList = new ArrayDeque<>(80);
    private final Map<Double, Integer> yRotModePopularity = new HashMap<Double, Integer>();
    private double xRotModeValue;
    private int xRotModeCount;
    private double yRotModeValue;
    private int yRotModeCount;
    private double shardLastXRot;
    private double shardLastYRot;
    private double shardModeX;
    private double shardModeY;
    private double shardDeltaDotsX;
    private double shardDeltaDotsY;
    private boolean shardAttackThisTick;
    private boolean shardSwungThisTick;
    private boolean shardRotationOnPreviousFlying = true;
    private boolean shardIdleFlyingSinceLastFrame;
    private UUID shardTargetId;
    private UUID shardLastTargetId;
    private long shardLastAttackMillis = -1L;
    private long shardCooldownResetMillis = -1L;
    private long shardLastDamageMillis = -1L;
    private long shardAnchorAttackMillis = -1L;
    private float shardPreAttackCooldown = 1.0f;
    private float shardAttackSpeed = 4.0f;
    private double shardLastHealth = Double.NaN;
    private final int[] memoryAir = new int[600];
    private final int[] memoryHits = new int[600];
    private int memoryIndex;
    private int memorySize;


    public UUID targetId() {
        synchronized (shardLock) {
            return shardTargetId;
        }
    }

    public UniverseFrame capture(Player bukkitPlayer, Entity target, float rawYaw, float rawPitch) {
        synchronized (shardLock) {
            float currentYaw = UniverseMouseMath.normalizeAngle(rawYaw);
            float currentPitch = Math.max(-90.0f, Math.min(90.0f, rawPitch));
            float deltaYaw = UniverseMouseMath.normalizeAngle(currentYaw - lastYaw);
            float deltaPitch = currentPitch - lastPitch;
            float accelYaw = UniverseMouseMath.calculateAcceleration(deltaYaw, lastDeltaYaw);
            float accelPitch = UniverseMouseMath.calculateAcceleration(deltaPitch, lastDeltaPitch);
            float jerkYaw = UniverseMouseMath.calculateJerk(accelYaw, lastAccelYaw);
            float jerkPitch = UniverseMouseMath.calculateJerk(accelPitch, lastAccelPitch);
            float gcdErrorYaw = UniverseMouseMath.calculateGCDError(deltaYaw, lastDeltaYaw);
            float gcdErrorPitch = UniverseMouseMath.calculateGCDError(deltaPitch, lastDeltaPitch);
            float[] shard = captureShardFeatures(bukkitPlayer, target, lastYaw, lastPitch,
                    currentYaw, currentPitch, deltaYaw, deltaPitch);
            lastYaw = currentYaw;
            lastPitch = currentPitch;
            lastDeltaYaw = deltaYaw;
            lastDeltaPitch = deltaPitch;
            lastAccelYaw = accelYaw;
            lastAccelPitch = accelPitch;
            return new UniverseFrame(deltaYaw, deltaPitch, accelYaw, accelPitch,
                    jerkYaw, jerkPitch, gcdErrorYaw, gcdErrorPitch, shard);
        }
    }
    public void onFlying(boolean hasRotation) {
        Object object = this.shardLock;
        synchronized (object) {
            if (!hasRotation) {
                this.shardIdleFlyingSinceLastFrame = true;
            }
        }
    }

    public void onAttack(UUID targetUuid) {
        Object object = this.shardLock;
        synchronized (object) {
            boolean anchorAlive;
            long now = System.currentTimeMillis();
            this.shardAttackThisTick = true;
            this.shardLastTargetId = this.shardTargetId;
            this.shardTargetId = targetUuid;
            float cooldownPeriod = 20.0f / this.shardAttackSpeed;
            this.shardPreAttackCooldown = UniverseState.clamp01((UniverseState.ticksSince(this.shardCooldownResetMillis, now) + 0.5f) / cooldownPeriod);
            boolean bl = anchorAlive = this.shardAnchorAttackMillis >= 0L && this.shardLastAttackMillis >= 0L && (double)(now - this.shardLastAttackMillis) / 50.0 < 100.0;
            if (!anchorAlive) {
                this.shardAnchorAttackMillis = now;
            }
            this.shardLastAttackMillis = now;
            this.shardCooldownResetMillis = now;
        }
    }

    public void onSwing() {
        Object object = this.shardLock;
        synchronized (object) {
            this.shardSwungThisTick = true;
            this.shardCooldownResetMillis = System.currentTimeMillis();
        }
    }

    public void onSlotChange() {
        Object object = this.shardLock;
        synchronized (object) {
            this.shardCooldownResetMillis = System.currentTimeMillis();
        }
    }

    public void resetShard() {
        Object object = this.shardLock;
        synchronized (object) {
            this.xRotModeList.clear();
            this.xRotModePopularity.clear();
            this.yRotModeList.clear();
            this.yRotModePopularity.clear();
            this.xRotModeValue = 0.0;
            this.xRotModeCount = 0;
            this.yRotModeValue = 0.0;
            this.yRotModeCount = 0;
            this.shardLastXRot = 0.0;
            this.shardLastYRot = 0.0;
            this.shardModeX = 0.0;
            this.shardModeY = 0.0;
            this.shardDeltaDotsX = 0.0;
            this.shardDeltaDotsY = 0.0;
            this.shardAttackThisTick = false;
            this.shardSwungThisTick = false;
            this.shardTargetId = null;
            this.shardLastTargetId = null;
            this.shardAnchorAttackMillis = -1L;
            this.shardLastHealth = Double.NaN;
        }
    }

    private float[] captureShardFeatures(Player bukkitPlayer, Entity target, float fromYaw, float fromPitch, float yaw, float pitch, float deltaYaw, float deltaPitch) {
        Object object = this.shardLock;
        synchronized (object) {
            long now = System.currentTimeMillis();
            float[] f = new float[UniverseFrame.SHARD_FEATURE_COUNT];
            boolean attackThisTick = this.shardAttackThisTick;
            boolean swungThisTick = this.shardSwungThisTick;
            this.shardAttackThisTick = false;
            this.shardSwungThisTick = false;
            f[0] = UniverseState.bool(this.shardRotationOnPreviousFlying && !this.shardIdleFlyingSinceLastFrame);
            this.shardRotationOnPreviousFlying = !this.shardIdleFlyingSinceLastFrame;
            this.shardIdleFlyingSinceLastFrame = false;
            f[1] = pitch;
            this.processShardAim(fromYaw, yaw, deltaYaw, deltaPitch);
            boolean modeYawValid = this.xRotModeCount > 15;
            boolean modePitchValid = this.yRotModeCount > 15;
            f[2] = (float)this.shardModeX;
            f[3] = (float)this.shardModeY;
            f[4] = UniverseState.bool(modeYawValid);
            f[5] = UniverseState.bool(modePitchValid);
            f[6] = modeYawValid ? (float)this.shardDeltaDotsX : 0.0f;
            f[7] = modePitchValid ? (float)this.shardDeltaDotsY : 0.0f;
            f[15] = UniverseState.bool(attackThisTick);
            f[16] = UniverseState.bool(swungThisTick);
            f[17] = UniverseState.ticksSince(this.shardLastAttackMillis, now);
            f[20] = this.shardAttackSpeed = UniverseState.readAttackSpeed(bukkitPlayer);
            float cooldown = attackThisTick ? this.shardPreAttackCooldown : UniverseState.clamp01((UniverseState.ticksSince(this.shardCooldownResetMillis, now) + 0.5f) / (20.0f / this.shardAttackSpeed));
            f[19] = UniverseState.isLegacyCombat(bukkitPlayer) ? 0.0f : cooldown;
            boolean anchorAlive = this.shardAnchorAttackMillis >= 0L && this.shardLastAttackMillis >= 0L && (double)(now - this.shardLastAttackMillis) / 50.0 < 100.0;
            f[18] = anchorAlive ? (float)Math.min(255.0, Math.floor((double)(now - this.shardAnchorAttackMillis) / 50.0)) : -255.0f;
            boolean targetValid = target != null && target.isValid() && !target.isDead() && target.getWorld().equals((Object)bukkitPlayer.getWorld());
            f[8] = UniverseState.bool(targetValid);
            if (targetValid) {
                this.captureShardTarget(f, bukkitPlayer, target, fromYaw, fromPitch, yaw, pitch, attackThisTick);
            }
            this.memoryAir[this.memoryIndex] = swungThisTick && !attackThisTick ? 1 : 0;
            this.memoryHits[this.memoryIndex] = attackThisTick ? 1 : 0;
            this.memoryIndex = (this.memoryIndex + 1) % 600;
            this.memorySize = Math.min(600, this.memorySize + 1);
            int air200 = this.memorySum(this.memoryAir, 200);
            int hits200 = this.memorySum(this.memoryHits, 200);
            int air600 = this.memorySum(this.memoryAir, 600);
            int hits600 = this.memorySum(this.memoryHits, 600);
            f[28] = air200;
            f[29] = hits200;
            f[30] = air600;
            f[31] = hits600;
            f[32] = air600 + hits600 == 0 ? 0.0f : (float)air600 / (float)(air600 + hits600);
            double health = bukkitPlayer.getHealth();
            float lost = Double.isNaN(this.shardLastHealth) ? 0.0f : (float)(this.shardLastHealth - health);
            this.shardLastHealth = health;
            if (lost > 0.0f) {
                f[33] = lost;
                this.shardLastDamageMillis = now;
            }
            f[34] = UniverseState.ticksSince(this.shardLastDamageMillis, now);
            f[35] = UniverseState.classifyItem(bukkitPlayer.getInventory().getItemInMainHand());
            f[36] = UniverseState.readPing(bukkitPlayer);
            return f;
        }
    }

    private void processShardAim(float fromYaw, float toYaw, float deltaYaw, float deltaPitch) {
        double[] mode;
        double yawUlp;
        double deltaYawAbs = Math.abs(deltaYaw);
        double deltaPitchAbs = Math.abs(deltaPitch);
        double divisorX = UniverseState.shardGcd(deltaYawAbs, this.shardLastXRot);
        if (deltaYawAbs > 0.0 && deltaYawAbs < 5.0 && divisorX > SHARD_MINIMUM_DIVISOR && (yawUlp = (double)Math.ulp(Math.max(Math.abs(fromYaw), Math.abs(toYaw)))) < divisorX * 0.0625) {
            if (this.shardLastXRot != 0.0) {
                UniverseState.runningModeAdd(this.xRotModeList, this.xRotModePopularity, divisorX, Math.max(0.001, yawUlp * 2.0));
            }
            this.shardLastXRot = deltaYawAbs;
        }
        double divisorY = UniverseState.shardGcd(deltaPitchAbs, this.shardLastYRot);
        if (deltaPitchAbs > 0.0 && deltaPitchAbs < 5.0 && divisorY > SHARD_MINIMUM_DIVISOR) {
            if (this.shardLastYRot != 0.0) {
                UniverseState.runningModeAdd(this.yRotModeList, this.yRotModePopularity, divisorY, 0.001);
            }
            this.shardLastYRot = deltaPitchAbs;
        }
        if (this.xRotModeList.size() > 15) {
            mode = UniverseState.runningModeUpdate(this.xRotModePopularity);
            this.xRotModeValue = mode[0];
            this.xRotModeCount = (int)mode[1];
            if (this.xRotModeCount > 15) {
                this.shardModeX = this.xRotModeValue;
            }
        }
        if (this.yRotModeList.size() > 15) {
            mode = UniverseState.runningModeUpdate(this.yRotModePopularity);
            this.yRotModeValue = mode[0];
            this.yRotModeCount = (int)mode[1];
            if (this.yRotModeCount > 15) {
                this.shardModeY = this.yRotModeValue;
            }
        }
        if (this.shardModeX > 0.0) {
            this.shardDeltaDotsX = deltaYawAbs / this.shardModeX;
        }
        if (this.shardModeY > 0.0) {
            this.shardDeltaDotsY = deltaPitchAbs / this.shardModeY;
        }
    }

    private static void runningModeAdd(ArrayDeque<Double> list, Map<Double, Integer> popularity, double value, double bucketWidth) {
        Double type;
        if (list.size() >= 80 && (type = list.poll()) != null) {
            Integer count = popularity.get(type);
            if (count == null || count == 1) {
                popularity.remove(type);
            } else {
                popularity.put(type, count - 1);
            }
        }
        for (Map.Entry<Double, Integer> entry : popularity.entrySet()) {
            if (!(Math.abs(entry.getKey() - value) < bucketWidth)) continue;
            entry.setValue(entry.getValue() + 1);
            list.add(entry.getKey());
            return;
        }
        popularity.put(value, 1);
        list.add(value);
    }

    private static double[] runningModeUpdate(Map<Double, Integer> popularity) {
        int max = 0;
        double mostPopular = 0.0;
        for (Map.Entry<Double, Integer> entry : popularity.entrySet()) {
            if (entry.getValue() <= max) continue;
            max = entry.getValue();
            mostPopular = entry.getKey();
        }
        return new double[]{mostPopular, max};
    }

    private static double shardGcd(double aInput, double bInput) {
        double temp;
        if (aInput == 0.0) {
            return 0.0;
        }
        double a = aInput;
        double b = bInput;
        if (a < b) {
            temp = a;
            a = b;
            b = temp;
        }
        while (b > SHARD_MINIMUM_DIVISOR) {
            temp = a - Math.floor(a / b) * b;
            a = b;
            b = temp;
        }
        return a;
    }

    private void captureShardTarget(float[] f, Player bukkitPlayer, Entity target, float fromYaw, float fromPitch, float yaw, float pitch, boolean attackThisTick) {
        Location feet = bukkitPlayer.getLocation();
        double px = feet.getX();
        double py = feet.getY();
        double pz = feet.getZ();
        f[13] = UniverseState.bool(this.shardTargetId != null && Objects.equals(this.shardTargetId, this.shardLastTargetId));
        BoundingBox box = target.getBoundingBox();
        List<Vector> lookDirs = UniverseState.possibleLookDirs(bukkitPlayer, fromYaw, fromPitch, yaw, pitch);
        f[9] = (float)UniverseState.minAngleToBox(px, py, pz, lookDirs, box);
        Location targetPos = target.getLocation();
        f[10] = (float)(targetPos.getX() - px);
        f[11] = (float)(targetPos.getY() - py);
        f[12] = (float)(targetPos.getZ() - pz);
        if (attackThisTick) {
            double maxDistance = Math.max(UniverseState.readAttribute(bukkitPlayer, INTERACTION_RANGE_ATTRIBUTE, 3.0) + 3.0, 6.0);
            Vector hitPoint = UniverseState.raytraceHitPoint(px, py, pz, lookDirs, box, maxDistance);
            f[21] = UniverseState.bool(hitPoint != null);
            f[22] = UniverseState.countRaytraceHits(px, py, pz, lookDirs, box, maxDistance);
            double[] axes = UniverseState.aimErrorAxes(bukkitPlayer, px, py, pz, yaw, pitch, box);
            f[23] = (float)axes[0];
            f[24] = (float)axes[1];
            if (hitPoint != null) {
                f[25] = (float)(hitPoint.getX() - box.getCenterX());
                f[26] = (float)(hitPoint.getY() - box.getCenterY());
                f[27] = (float)(hitPoint.getZ() - box.getCenterZ());
            }
        }
        f[14] = UniverseState.countTargetsInRange(bukkitPlayer, px, py, pz);
    }

    private static List<Vector> possibleLookDirs(Player bukkitPlayer, float lastYaw, float lastPitch, float yaw, float pitch) {
        ClientVersion version = UniverseState.clientVersion(bukkitPlayer);
        ArrayList<Vector> dirs = new ArrayList<Vector>(3);
        dirs.add(UniverseState.lookVector(yaw, pitch));
        if (version != null && version.isOlderThan(ClientVersion.V_1_8)) {
            return dirs;
        }
        dirs.add(UniverseState.lookVector(lastYaw, pitch));
        if (version == null || version.isNewerThanOrEquals(ClientVersion.V_1_9)) {
            dirs.add(UniverseState.lookVector(lastYaw, lastPitch));
        }
        return dirs;
    }

    private static Vector lookVector(float yaw, float pitch) {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(-yaw);
        double pitchCos = Math.cos(pitchRad);
        Vector look = new Vector(Math.sin(yawRad) * pitchCos, -Math.sin(pitchRad), Math.cos(yawRad) * pitchCos);
        double length = look.length();
        return length == 0.0 ? look : look.multiply(1.0 / length);
    }

    private static double minAngleToBox(double px, double py, double pz, List<Vector> lookDirs, BoundingBox box) {
        double[][] points = new double[][]{{box.getCenterX(), box.getCenterY(), box.getCenterZ()}, {box.getMinX(), box.getMinY(), box.getMinZ()}, {box.getMaxX(), box.getMinY(), box.getMinZ()}, {box.getMinX(), box.getMaxY(), box.getMinZ()}, {box.getMaxX(), box.getMaxY(), box.getMinZ()}, {box.getMinX(), box.getMinY(), box.getMaxZ()}, {box.getMaxX(), box.getMinY(), box.getMaxZ()}, {box.getMinX(), box.getMaxY(), box.getMaxZ()}, {box.getMaxX(), box.getMaxY(), box.getMaxZ()}};
        double minAngle = Double.MAX_VALUE;
        for (Vector lookVec : lookDirs) {
            for (double eye : SHARD_POSSIBLE_EYE_HEIGHTS) {
                double ey = py + eye;
                for (double[] point : points) {
                    double dx = point[0] - px;
                    double dy = point[1] - ey;
                    double dz = point[2] - pz;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (len < 0.001) {
                        return 0.0;
                    }
                    double dot = lookVec.getX() * (dx / len) + lookVec.getY() * (dy / len) + lookVec.getZ() * (dz / len);
                    minAngle = Math.min(minAngle, Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot)))));
                }
            }
        }
        return minAngle;
    }

    private static Vector raytraceHitPoint(double px, double py, double pz, List<Vector> lookDirs, BoundingBox box, double maxDistance) {
        for (Vector lookVec : lookDirs) {
            for (double eye : SHARD_POSSIBLE_EYE_HEIGHTS) {
                Vector origin = new Vector(px, py + eye, pz);
                if (UniverseState.isVecInside(box, origin)) {
                    return origin;
                }
                RayTraceResult result = box.rayTrace(origin, lookVec, maxDistance);
                if (result == null) continue;
                return result.getHitPosition();
            }
        }
        return null;
    }

    private static int countRaytraceHits(double px, double py, double pz, List<Vector> lookDirs, BoundingBox box, double maxDistance) {
        int hits = 0;
        for (Vector lookVec : lookDirs) {
            for (double eye : SHARD_POSSIBLE_EYE_HEIGHTS) {
                Vector origin = new Vector(px, py + eye, pz);
                if (!UniverseState.isVecInside(box, origin) && box.rayTrace(origin, lookVec, maxDistance) == null) continue;
                ++hits;
            }
        }
        return hits;
    }

    private static double[] aimErrorAxes(Player bukkitPlayer, double px, double py, double pz, float yaw, float pitch, BoundingBox box) {
        double[] out = new double[2];
        double eyeHeight = bukkitPlayer.isSwimming() || bukkitPlayer.isGliding() ? 0.4 : (bukkitPlayer.isSneaking() ? 1.27 : 1.62);
        double dx = box.getCenterX() - px;
        double dy = box.getCenterY() - (py + eyeHeight);
        double dz = box.getCenterZ() - pz;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.001) {
            return out;
        }
        double yawError = (double)yaw - Math.toDegrees(Math.atan2(-dx, dz));
        yawError -= Math.floor(yawError / 360.0 + 0.5) * 360.0;
        double pitchError = (double)pitch - Math.toDegrees(-Math.atan2(dy, horizontal));
        double halfWidth = Math.max(box.getWidthX(), box.getWidthZ()) / 2.0;
        double halfHeight = box.getHeight() / 2.0;
        double angularWidth = Math.toDegrees(Math.atan2(halfWidth, distance));
        double angularHeight = Math.toDegrees(Math.atan2(halfHeight, distance));
        if (angularWidth > 1.0E-6) {
            out[0] = yawError / angularWidth;
        }
        if (angularHeight > 1.0E-6) {
            out[1] = pitchError / angularHeight;
        }
        return out;
    }

    private static boolean isVecInside(BoundingBox box, Vector vec) {
        return vec.getX() > box.getMinX() && vec.getX() < box.getMaxX() && vec.getY() > box.getMinY() && vec.getY() < box.getMaxY() && vec.getZ() > box.getMinZ() && vec.getZ() < box.getMaxZ();
    }

    private static int countTargetsInRange(Player bukkitPlayer, double px, double py, double pz) {
        int count = 0;
        for (Entity entity : bukkitPlayer.getNearbyEntities(6.0, 6.0, 6.0)) {
            double dz;
            double dy;
            Location pos;
            double dx;
            Player other;
            if (!(entity instanceof LivingEntity) || entity.isDead() || entity instanceof Player && (other = (Player)entity).getGameMode() == GameMode.SPECTATOR || !((dx = (pos = entity.getLocation()).getX() - px) * dx + (dy = pos.getY() - py) * dy + (dz = pos.getZ() - pz) * dz <= 36.0)) continue;
            ++count;
        }
        return count;
    }

    private static int classifyItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 1;
        }
        Material type = item.getType();
        switch (type.name()) {
            case "END_CRYSTAL": {
                return 3;
            }
            case "RESPAWN_ANCHOR": {
                return 5;
            }
            case "GLOWSTONE": {
                return 6;
            }
            case "TOTEM_OF_UNDYING": {
                return 7;
            }
            case "SHIELD": {
                return 12;
            }
            case "OBSIDIAN":
            case "CRYING_OBSIDIAN":
            case "BEDROCK": {
                return 4;
            }
            case "BOW":
            case "CROSSBOW":
            case "TRIDENT": {
                return 9;
            }
            case "ENDER_PEARL":
            case "SNOWBALL":
            case "EGG": {
                return 10;
            }
        }
        String name = type.name();
        if (name.endsWith("_SWORD") || name.endsWith("_AXE") && !name.endsWith("_PICKAXE")) {
            return 2;
        }
        if (type.isEdible()) {
            return 11;
        }
        if (type.isBlock()) {
            return 8;
        }
        return 0;
    }

    private int memorySum(int[] ring, int count) {
        int n = Math.min(count, this.memorySize);
        int sum = 0;
        for (int i = 1; i <= n; ++i) {
            sum += ring[(this.memoryIndex - i + 600) % 600];
        }
        return sum;
    }

    private static float ticksSince(long millis, long now) {
        if (millis < 0L) {
            return 255.0f;
        }
        return (float)Math.min(255.0, Math.floor((double)(now - millis) / 50.0));
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float bool(boolean value) {
        return value ? 1.0f : 0.0f;
    }

    private static ClientVersion clientVersion(Player bukkitPlayer) {
        try {
            return PacketEvents.getAPI().getPlayerManager().getClientVersion((Object)bukkitPlayer);
        }
        catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean isLegacyCombat(Player bukkitPlayer) {
        Boolean modern = modernCombatServer;
        if (modern == null) {
            try {
                modern = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9);
            }
            catch (RuntimeException exception) {
                modern = Boolean.TRUE;
            }
            modernCombatServer = modern;
        }
        ClientVersion version = UniverseState.clientVersion(bukkitPlayer);
        return modern == false || version != null && version.isOlderThan(ClientVersion.V_1_9);
    }

    private static float readAttackSpeed(Player bukkitPlayer) {
        double value = UniverseState.readAttribute(bukkitPlayer, ATTACK_SPEED_ATTRIBUTE, 4.0);
        return value > 0.0 ? (float)value : 4.0f;
    }

    private static double readAttribute(Player bukkitPlayer, Attribute attribute, double fallback) {
        if (attribute == null) {
            return fallback;
        }
        try {
            AttributeInstance instance = bukkitPlayer.getAttribute(attribute);
            return instance == null ? fallback : instance.getValue();
        }
        catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static float readPing(Player bukkitPlayer) {
        try {
            return PacketEvents.getAPI().getPlayerManager().getPing((Object)bukkitPlayer);
        }
        catch (RuntimeException exception) {
            return 0.0f;
        }
    }

    private static Attribute resolveAttribute(String ... names) {
        for (String name : names) {
            try {
                Field field = Attribute.class.getField(name);
                Object value = field.get(null);
                if (!(value instanceof Attribute)) continue;
                Attribute attribute = (Attribute)value;
                return attribute;
            }
            catch (ReflectiveOperationException | RuntimeException exception) {
                // empty catch block
            }
        }
        return null;
    }

}
