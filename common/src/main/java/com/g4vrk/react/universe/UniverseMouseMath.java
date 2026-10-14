package com.g4vrk.react.universe;


public final class UniverseMouseMath {
    private static final double MINIMUM_DIVISOR = Math.pow(0.2, 3.0) * 8.0 * 0.15 - 0.001;
    private static final double ROTATION_EPSILON = 1.0E-6;

    public static float calculateAcceleration(float currentDelta, float previousDelta) {
        return currentDelta - previousDelta;
    }

    public static float calculateJerk(float currentAccel, float previousAccel) {
        return currentAccel - previousAccel;
    }

    public static float calculateGCDError(float currentDelta, float previousDelta) {
        double current = Math.abs(currentDelta);
        double previous = Math.abs(previousDelta);
        if (current < MINIMUM_DIVISOR || previous < MINIMUM_DIVISOR) {
            return 0.0f;
        }
        double gcd = UniverseMouseMath.gcd(current, previous);
        if (gcd < MINIMUM_DIVISOR) {
            return 0.0f;
        }
        double snapped = (double)Math.round(current / gcd) * gcd;
        double error = Math.abs(current - snapped);
        return (float)(error / gcd);
    }

    public static float calculateGrid(float currentDelta, float previousDelta) {
        double current = Math.abs(currentDelta);
        double previous = Math.abs(previousDelta);
        if (current < MINIMUM_DIVISOR || previous < MINIMUM_DIVISOR) {
            return 0.0f;
        }
        double gcd = UniverseMouseMath.gcd(current, previous);
        return gcd < MINIMUM_DIVISOR ? 0.0f : (float)gcd;
    }

    public static float normalizeAngle(float angle) {
        float normalized = angle % 360.0f;
        if (normalized <= -180.0f) {
            normalized += 360.0f;
        } else if (normalized > 180.0f) {
            normalized -= 360.0f;
        }
        return normalized;
    }

    public static double gcd(double aInput, double bInput) {
        double temp;
        double a = Math.abs(aInput);
        double b = Math.abs(bInput);
        if (a < 1.0E-6 || b < 1.0E-6) {
            return 0.0;
        }
        if (a < b) {
            temp = a;
            a = b;
            b = temp;
        }
        while (b > MINIMUM_DIVISOR) {
            temp = a - Math.floor(a / b) * b;
            a = b;
            b = temp;
        }
        return a;
    }

    public static float distanceToGrid(float delta, float grid) {
        double absDelta = Math.abs(delta);
        double absGrid = Math.abs(grid);
        if (absDelta < MINIMUM_DIVISOR || absGrid < MINIMUM_DIVISOR) {
            return 0.0f;
        }
        double snapped = (double)Math.round(absDelta / absGrid) * absGrid;
        return (float)Math.abs(absDelta - snapped);
    }

    public static float getMinimumDivisor() {
        return (float)MINIMUM_DIVISOR;
    }

    private UniverseMouseMath() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
