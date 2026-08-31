package dev.fauza.tdeh.math;

import dev.fauza.tdeh.model.EulerRotation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RotationMathTest {

    private static final float TOLERANCE = 1.0e-5f;

    @Test
    void identityRotationLeavesVectorsAlone() {
        assertRotates(EulerRotation.NONE, new Vector3f(1, 0, 0), 1, 0, 0);
        assertRotates(EulerRotation.NONE, new Vector3f(0, 1, 0), 0, 1, 0);
    }

    @Test
    void yawTurnsAroundTheVerticalAxis() {
        // Right-hand rule about +Y: +90 degrees sends +X to -Z.
        assertRotates(new EulerRotation(90, 0, 0), new Vector3f(1, 0, 0), 0, 0, -1);
        assertRotates(new EulerRotation(180, 0, 0), new Vector3f(1, 0, 0), -1, 0, 0);
        assertRotates(new EulerRotation(90, 0, 0), new Vector3f(0, 1, 0), 0, 1, 0);
    }

    @Test
    void pitchTurnsAroundTheXAxis() {
        assertRotates(new EulerRotation(0, 90, 0), new Vector3f(0, 1, 0), 0, 0, 1);
    }

    @Test
    void rollTurnsAroundTheZAxis() {
        assertRotates(new EulerRotation(0, 0, 90), new Vector3f(1, 0, 0), 0, 1, 0);
    }

    @Test
    void negativeAnglesTurnTheOtherWay() {
        assertRotates(new EulerRotation(-90, 0, 0), new Vector3f(1, 0, 0), 0, 0, 1);
    }

    @Test
    void anglesBeyondAFullTurnAreEquivalentToTheirWrappedValue() {
        // The quaternion itself may come back negated (q and -q are the same rotation), so this
        // compares the effect on a vector rather than the raw components.
        Vector3f wrapped = rotate(new EulerRotation(450, 0, 0), new Vector3f(1, 0, 0));
        Vector3f plain = rotate(new EulerRotation(90, 0, 0), new Vector3f(1, 0, 0));

        assertEquals(plain.x, wrapped.x, TOLERANCE);
        assertEquals(plain.y, wrapped.y, TOLERANCE);
        assertEquals(plain.z, wrapped.z, TOLERANCE);
    }

    @Test
    void combinedRotationsApplyInYawPitchRollOrder() {
        // The composed matrix is Ry * Rx * Rz, so against a vector the roll lands first and the yaw
        // last: +Z is tipped to -Y by the pitch, and the yaw then leaves that vertical vector alone.
        assertRotates(new EulerRotation(90, 90, 0), new Vector3f(0, 0, 1), 0, -1, 0);
    }

    @Test
    void producedQuaternionIsNormalised() {
        Quaternionf quaternion = RotationMath.toQuaternion(new EulerRotation(33, 77, 129));

        assertEquals(1.0f, (float) Math.sqrt(quaternion.lengthSquared()), TOLERANCE);
    }

    private static Vector3f rotate(EulerRotation rotation, Vector3f vector) {
        return RotationMath.toQuaternion(rotation).transform(new Vector3f(vector));
    }

    private static void assertRotates(EulerRotation rotation, Vector3f input, float x, float y, float z) {
        Vector3f result = rotate(rotation, input);
        assertEquals(x, result.x, TOLERANCE, "x of " + result);
        assertEquals(y, result.y, TOLERANCE, "y of " + result);
        assertEquals(z, result.z, TOLERANCE, "z of " + result);
    }
}
