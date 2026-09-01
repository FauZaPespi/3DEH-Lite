package dev.fauza.tdeh.math;

import dev.fauza.tdeh.model.EulerRotation;
import dev.fauza.tdeh.model.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockPivotTest {

    private static final float TOLERANCE = 1.0e-5f;

    @Test
    void anUntouchedBlockIsPulledBackByHalfItsSize() {
        assertCentering(Vec3.ZERO, Vec3.ONE, EulerRotation.NONE, -0.5f, -0.5f, -0.5f);
    }

    @Test
    void theShiftFollowsTheScale() {
        assertCentering(Vec3.ZERO, new Vec3(2, 2, 2), EulerRotation.NONE, -1, -1, -1);
        assertCentering(Vec3.ZERO, new Vec3(4, 1, 0.5), EulerRotation.NONE, -2, -0.5f, -0.25f);
    }

    @Test
    void theOperatorsOwnTranslationStillApplies() {
        assertCentering(new Vec3(0, 3, 0), Vec3.ONE, EulerRotation.NONE, -0.5f, 2.5f, -0.5f);
    }

    @Test
    void theShiftTurnsWithTheBlockSoItSpinsInPlace() {
        // Yaw +90 sends +X to -Z, which turns the (-0.5,-0.5,-0.5) shift into (-0.5,-0.5,+0.5):
        // the centre stays on the anchor instead of the block swinging around its corner.
        assertCentering(Vec3.ZERO, Vec3.ONE, new EulerRotation(90, 0, 0), -0.5f, -0.5f, 0.5f);
        assertCentering(Vec3.ZERO, Vec3.ONE, new EulerRotation(180, 0, 0), 0.5f, -0.5f, 0.5f);
    }

    private static void assertCentering(
            Vec3 translation, Vec3 scale, EulerRotation rotation, float x, float y, float z) {
        Vector3f result = BlockPivot.centeringTranslation(translation, scale, rotation);
        assertEquals(x, result.x, TOLERANCE, "x");
        assertEquals(y, result.y, TOLERANCE, "y");
        assertEquals(z, result.z, TOLERANCE, "z");
    }
}
