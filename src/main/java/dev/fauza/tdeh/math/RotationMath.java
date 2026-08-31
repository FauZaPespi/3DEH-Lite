package dev.fauza.tdeh.math;

import dev.fauza.tdeh.model.EulerRotation;
import org.joml.Quaternionf;

/**
 * Converts the Euler angles admins type into the quaternion the Display transformation wants.
 *
 * <p>JOML ships with the server and is a pure math library, so using it here keeps this class
 * testable without a running server.
 *
 * <p>Convention: yaw turns around Y, pitch around X, roll around Z, applied in that intrinsic
 * order (Y-X-Z), which is the same order vanilla composes entity rotations in. Angles are degrees
 * and follow the right-hand rule, so a positive yaw turns the display counter-clockwise seen from
 * above.
 */
public final class RotationMath {

    private RotationMath() {
    }

    public static Quaternionf toQuaternion(EulerRotation rotation) {
        return new Quaternionf().rotationYXZ(
                (float) Math.toRadians(rotation.yaw()),
                (float) Math.toRadians(rotation.pitch()),
                (float) Math.toRadians(rotation.roll()));
    }
}
