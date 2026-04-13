package ai.solar.kirill.utils.game;


public class MouseCalculator {
    private static final float GCD_EPSILON = 0.0001f;

    public static float calculateAcceleration(float currentDelta, float previousDelta) {
        return currentDelta - previousDelta;
    }

    public static float calculateJerk(float currentAccel, float previousAccel) {
        return currentAccel - previousAccel;
    }

    public static float calculateGCDError(float delta) {
        if (Math.abs(delta) < GCD_EPSILON) {
            return 0.0f;
        }

        float gcd = calculateGCD(Math.abs(delta));
        float remainder = Math.abs(delta) % gcd;
        return remainder / gcd;
    }

    private static float calculateGCD(float a) {
        a = Math.abs(a);
        float b = 0.1f;

        while (b > GCD_EPSILON) {
            float temp = a % b;
            a = b;
            b = temp;
        }

        return a;
    }

    public static float normalizeAngle(float angle) {
        angle = angle % 360.0f;
        if (angle > 180.0f) {
            angle -= 360.0f;
        } else if (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }
}