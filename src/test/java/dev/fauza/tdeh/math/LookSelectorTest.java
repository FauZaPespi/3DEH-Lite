package dev.fauza.tdeh.math;

import dev.fauza.tdeh.math.LookSelector.Candidate;
import dev.fauza.tdeh.model.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookSelectorTest {

    private static final Vec3 EYE = new Vec3(0, 0, 0);
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);
    private static final double MAX_DISTANCE = 24;
    private static final double MAX_ANGLE = 12;

    @Test
    void picksTheHologramInFrontOfThePlayer() {
        Candidate ahead = new Candidate("ahead", new Vec3(0, 0, 10));

        assertEquals(Optional.of(ahead), select(List.of(ahead)));
    }

    @Test
    void ignoresWhatIsBehindThePlayer() {
        Candidate behind = new Candidate("behind", new Vec3(0, 0, -10));

        assertTrue(select(List.of(behind)).isEmpty());
    }

    @Test
    void ignoresWhatIsBeyondTheMaximumDistance() {
        Candidate far = new Candidate("far", new Vec3(0, 0, 40));

        assertTrue(select(List.of(far)).isEmpty());
        assertEquals(Optional.of(far),
                LookSelector.select(EYE, FORWARD, List.of(far), 50, MAX_ANGLE));
    }

    @Test
    void ignoresWhatIsOutsideTheCone() {
        // 10 blocks ahead and 10 to the side is 45 degrees off, well outside a 12 degree cone.
        Candidate offAxis = new Candidate("off-axis", new Vec3(10, 0, 10));

        assertTrue(select(List.of(offAxis)).isEmpty());
    }

    @Test
    void prefersTheBetterAlignedCandidateOverTheNearerOne() {
        Candidate nearButOff = new Candidate("near-off", new Vec3(0.6, 0, 5));
        Candidate farButCentred = new Candidate("far-centred", new Vec3(0, 0, 20));

        assertEquals(Optional.of(farButCentred), select(List.of(nearButOff, farButCentred)));
    }

    @Test
    void breaksAlignmentTiesByDistance() {
        Candidate near = new Candidate("near", new Vec3(0, 0, 5));
        Candidate far = new Candidate("far", new Vec3(0, 0, 20));

        assertEquals(Optional.of(near), select(List.of(far, near)));
        assertEquals(Optional.of(near), select(List.of(near, far)));
    }

    @Test
    void aCandidateAtTheEyePositionIsSelectedImmediately() {
        Candidate here = new Candidate("here", EYE);

        assertEquals(Optional.of(here), select(List.of(here)));
    }

    @Test
    void noCandidatesYieldsNoSelection() {
        assertTrue(select(List.of()).isEmpty());
    }

    @Test
    void aZeroLengthLookVectorYieldsNoSelection() {
        Candidate ahead = new Candidate("ahead", new Vec3(0, 0, 10));

        assertTrue(LookSelector.select(EYE, Vec3.ZERO, List.of(ahead), MAX_DISTANCE, MAX_ANGLE).isEmpty());
    }

    @Test
    void theLookVectorDoesNotNeedToBeNormalised() {
        Candidate ahead = new Candidate("ahead", new Vec3(0, 0, 10));

        assertEquals(Optional.of(ahead),
                LookSelector.select(EYE, new Vec3(0, 0, 37), List.of(ahead), MAX_DISTANCE, MAX_ANGLE));
    }

    @Test
    void selectionWorksOnEveryAxis() {
        Candidate above = new Candidate("above", new Vec3(0, 8, 0));

        assertEquals(Optional.of(above),
                LookSelector.select(EYE, new Vec3(0, 1, 0), List.of(above), MAX_DISTANCE, MAX_ANGLE));
    }

    private static Optional<Candidate> select(List<Candidate> candidates) {
        return LookSelector.select(EYE, FORWARD, candidates, MAX_DISTANCE, MAX_ANGLE);
    }
}
