package dev.fauza.tdeh.math;

import dev.fauza.tdeh.model.EulerRotation;
import dev.fauza.tdeh.model.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Centres a block display on its entity position.
 *
 * <p>A block display is the odd one out: text and item displays are drawn around their entity
 * position, but a block model spans 0..1 from it, so its pivot is the bottom-north-west corner.
 * Left uncorrected, a block part renders half a block off its anchor and, under any billboard mode
 * other than {@code FIXED}, orbits that corner instead of spinning in place.
 *
 * <p>Vanilla maps a vertex to {@code translation + leftRotation * (scale * vertex)}, so shifting the
 * model by half its scaled size <em>before</em> the rotation puts the centre on the anchor and keeps
 * it there through any rotation.
 */
public final class BlockPivot {

    private BlockPivot() {
    }

    /** @return the translation to hand the display, i.e. the part's own translation plus the shift. */
    public static Vector3f centeringTranslation(Vec3 translation, Vec3 scale, EulerRotation rotation) {
        Quaternionf leftRotation = RotationMath.toQuaternion(rotation);
        Vector3f halfExtent = new Vector3f((float) scale.x(), (float) scale.y(), (float) scale.z())
                .mul(0.5f);
        leftRotation.transform(halfExtent);

        return new Vector3f((float) translation.x(), (float) translation.y(), (float) translation.z())
                .sub(halfExtent);
    }
}
