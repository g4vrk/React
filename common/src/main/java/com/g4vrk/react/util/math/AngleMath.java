package com.g4vrk.react.util.math;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AngleMath {

    private static final float GCD_EPSILON = 0.0001f;

    public static float calculateAcceleration(
            final float currentDelta,
            final float previousDelta
    ) {
        return currentDelta - previousDelta;
    }

    public static float calculateJerk(
            final float currentAccel,
            final float previousAccel
    ) {
        return currentAccel - previousAccel;
    }

    public static float calculateGCDError(
            final float delta
    ) {
        if (Math.abs(delta) < GCD_EPSILON) {
            return 0.0f;
        }

        float gcd = calculateGCD(Math.abs(delta));
        float remainder = Math.abs(delta) % gcd;

        return remainder / gcd;
    }

    private static float calculateGCD(
            float a
    ) {
        a = Math.abs(a);
        float b = 0.1f;

        while (b > GCD_EPSILON) {
            float temp = a % b;
            a = b;
            b = temp;
        }

        return a;
    }

    public static float normalizeAngle(
            float angle
    ) {
        angle = angle % 360.0f;
        if (angle > 180.0f) {
            angle -= 360.0f;
        } else if (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    public static float normalizeDelta(
            float delta
    ) {
        delta %= 360.0f;

        if (delta > 180.0f) delta -= 360.0f;
        else if (delta < -180.0f) delta += 360.0f;

        return delta;
    }
}