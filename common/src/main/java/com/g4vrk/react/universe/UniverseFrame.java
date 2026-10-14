package com.g4vrk.react.universe;

import java.util.Arrays;
import java.util.Locale;

public final class UniverseFrame {
    public static final int DATASET_FEATURE_COUNT = 45;
    public static final int INFERENCE_FEATURE_COUNT = 44;
    public static final String BASE_HEADER = "delta_yaw,delta_pitch,accel_yaw,accel_pitch,jerk_yaw,jerk_pitch,gcd_error_yaw,gcd_error_pitch";
    public static final int ROTATION_PRESENT = 0;
    public static final int PITCH = 1;
    public static final int MODE_YAW = 2;
    public static final int MODE_PITCH = 3;
    public static final int MODE_YAW_VALID = 4;
    public static final int MODE_PITCH_VALID = 5;
    public static final int DELTA_DOTS_YAW = 6;
    public static final int DELTA_DOTS_PITCH = 7;
    public static final int TARGET_VALID = 8;
    public static final int TARGET_AIM_ANGLE = 9;
    public static final int TARGET_REL_X = 10;
    public static final int TARGET_REL_Y = 11;
    public static final int TARGET_REL_Z = 12;
    public static final int SAME_TARGET_AS_PREV_ATTACK = 13;
    public static final int TARGETS_IN_RANGE_COUNT = 14;
    public static final int ATTACK_THIS_TICK = 15;
    public static final int SWUNG_THIS_TICK = 16;
    public static final int TICKS_SINCE_ATTACK = 17;
    public static final int TICKS_TO_ATTACK = 18;
    public static final int ATTACK_COOLDOWN_PROGRESS = 19;
    public static final int ATTACK_SPEED = 20;
    public static final int RAYTRACE_HIT = 21;
    public static final int RAYTRACE_DIRS_HIT_COUNT = 22;
    public static final int AIM_ERROR_YAW = 23;
    public static final int AIM_ERROR_PITCH = 24;
    public static final int HIT_POINT_REL_X = 25;
    public static final int HIT_POINT_REL_Y = 26;
    public static final int HIT_POINT_REL_Z = 27;
    public static final int AIR_200 = 28;
    public static final int HITS_200 = 29;
    public static final int AIR_600 = 30;
    public static final int HITS_600 = 31;
    public static final int MISS_SHARE = 32;
    public static final int DAMAGE_TAKEN_THIS_TICK = 33;
    public static final int TICKS_SINCE_DAMAGE = 34;
    public static final int HELD_ITEM_CLASS = 35;
    public static final int PLAYER_PING = 36;
    public static final String[] SHARD_FEATURE_NAMES = new String[]{"rotation_present", "pitch", "mode_yaw", "mode_pitch", "mode_yaw_valid", "mode_pitch_valid", "delta_dots_yaw", "delta_dots_pitch", "target_valid", "target_aim_angle", "target_rel_x", "target_rel_y", "target_rel_z", "same_target_as_prev_attack", "targets_in_range_count", "attack_this_tick", "swung_this_tick", "ticks_since_attack", "ticks_to_attack", "attack_cooldown_progress", "attack_speed", "raytrace_hit", "raytrace_dirs_hit_count", "aim_error_yaw", "aim_error_pitch", "hit_point_rel_x", "hit_point_rel_y", "hit_point_rel_z", "air_200", "hits_200", "air_600", "hits_600", "miss_share", "damage_taken_this_tick", "ticks_since_damage", "held_item_class", "player_ping"};
    public static final int SHARD_FEATURE_COUNT = SHARD_FEATURE_NAMES.length;


    private final float[] features;

    public UniverseFrame(float deltaYaw, float deltaPitch, float accelYaw, float accelPitch,
                         float jerkYaw, float jerkPitch, float gcdErrorYaw, float gcdErrorPitch,
                         float[] shard) {
        if (shard != null && shard.length != SHARD_FEATURE_COUNT) {
            throw new IllegalArgumentException("Expected 37 SHARD features");
        }
        features = new float[DATASET_FEATURE_COUNT];
        features[0] = deltaYaw;
        features[1] = deltaPitch;
        features[2] = accelYaw;
        features[3] = accelPitch;
        features[4] = jerkYaw;
        features[5] = jerkPitch;
        features[6] = gcdErrorYaw;
        features[7] = gcdErrorPitch;
        if (shard == null) {
            features[8 + TICKS_TO_ATTACK] = -255.0f;
        } else {
            System.arraycopy(shard, 0, features, 8, SHARD_FEATURE_COUNT);
        }
    }

    public static UniverseFrame fromFeatures(float[] values) {
        if (values.length != DATASET_FEATURE_COUNT) {
            throw new IllegalArgumentException("Expected 45 UNIVERSE features");
        }
        return new UniverseFrame(values[0], values[1], values[2], values[3], values[4],
                values[5], values[6], values[7], Arrays.copyOfRange(values, 8, values.length));
    }

    public float[] datasetFeatures() {
        return features.clone();
    }

    public float[] inferenceFeatures() {
        // Raveon omits only the last SHARD field (PLAYER_PING).
        return Arrays.copyOf(features, INFERENCE_FEATURE_COUNT);
    }

    public static String csvHeader() {
        return "is_cheating," + BASE_HEADER + "," + String.join(",", SHARD_FEATURE_NAMES);
    }

    public String toCsvRow(boolean cheater) {
        StringBuilder row = new StringBuilder(cheater ? "1" : "0");
        for (float value : features) {
            row.append(',').append(String.format(Locale.US, "%.6f", value));
        }
        return row.toString();
    }
}
