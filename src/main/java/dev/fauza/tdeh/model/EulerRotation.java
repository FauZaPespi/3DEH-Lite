package dev.fauza.tdeh.model;

/**
 * Rotation expressed as intrinsic Euler angles in degrees, which is what admins type in
 * commands. Conversion to the quaternion the Display API expects lives in
 * {@code dev.fauza.tdeh.math.RotationMath}.
 */
public record EulerRotation(double yaw, double pitch, double roll) {

    public static final EulerRotation NONE = new EulerRotation(0, 0, 0);

    public EulerRotation {
        requireFinite(yaw, "yaw");
        requireFinite(pitch, "pitch");
        requireFinite(roll, "roll");
    }

    public boolean isIdentity() {
        return normalized().equals(NONE);
    }

    /** Wraps every angle into [0, 360) so equivalent rotations compare equal. */
    public EulerRotation normalized() {
        return new EulerRotation(wrap(yaw), wrap(pitch), wrap(roll));
    }

    private static double wrap(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped < 0) {
            wrapped += 360.0;
        }
        // -0.0 % 360 yields -0.0, which is not equal to 0.0 under record equality.
        return wrapped == 0.0 ? 0.0 : wrapped;
    }

    private static void requireFinite(double value, String component) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Rotation component " + component + " must be finite, got " + value);
        }
    }
}
