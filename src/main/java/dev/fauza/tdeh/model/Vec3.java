package dev.fauza.tdeh.model;

/**
 * A plain three-component vector, used for part offsets, scale and translation.
 * Kept free of JOML so the model stays testable without a server on the classpath.
 */
public record Vec3(double x, double y, double z) {

    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public static final Vec3 ONE = new Vec3(1, 1, 1);

    public Vec3 {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public static Vec3 of(double uniform) {
        return new Vec3(uniform, uniform, uniform);
    }

    public Vec3 plus(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    private static void requireFinite(double value, String component) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Vector component " + component + " must be finite, got " + value);
        }
    }
}
