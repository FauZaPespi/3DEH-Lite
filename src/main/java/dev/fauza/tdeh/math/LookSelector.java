package dev.fauza.tdeh.math;

import dev.fauza.tdeh.model.Vec3;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Picks the hologram a player is looking at.
 *
 * <p>Display entities have no collision box, so a raytrace does not reliably hit them. Selection is
 * therefore angular: candidates inside a cone around the player's look vector are ranked by how
 * close they are to the centre of that cone, and ties are broken by distance so the nearest of two
 * aligned holograms wins.
 *
 * <p>Pure geometry, no Bukkit types, so the caller converts the player's eye location and direction
 * on the region thread and this runs anywhere.
 */
public final class LookSelector {

    private LookSelector() {
    }

    /** A selectable hologram reduced to what the geometry needs. */
    public record Candidate(String name, Vec3 position) {
        public Candidate {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(position, "position");
        }
    }

    /**
     * @param eye         the player's eye position
     * @param direction   the player's look vector, not required to be normalised
     * @param candidates  holograms in the player's world
     * @param maxDistance how far the selection reaches, in blocks
     * @param maxAngleDeg half-angle of the selection cone, in degrees
     */
    public static Optional<Candidate> select(Vec3 eye,
                                             Vec3 direction,
                                             Collection<Candidate> candidates,
                                             double maxDistance,
                                             double maxAngleDeg) {
        Objects.requireNonNull(eye, "eye");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(candidates, "candidates");

        double dirLength = length(direction);
        if (dirLength == 0 || candidates.isEmpty()) {
            return Optional.empty();
        }
        double minCos = Math.cos(Math.toRadians(Math.max(0, maxAngleDeg)));

        Candidate best = null;
        double bestCos = -1;
        double bestDistance = Double.MAX_VALUE;

        for (Candidate candidate : candidates) {
            double dx = candidate.position().x() - eye.x();
            double dy = candidate.position().y() - eye.y();
            double dz = candidate.position().z() - eye.z();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > maxDistance) {
                continue;
            }
            if (distance == 0) {
                // Standing inside it: nothing can be a closer match.
                return Optional.of(candidate);
            }
            double cos = (dx * direction.x() + dy * direction.y() + dz * direction.z()) / (distance * dirLength);
            if (cos < minCos) {
                continue;
            }
            // Prefer the better-aligned candidate; only fall back to distance when alignment ties.
            if (cos > bestCos || (cos == bestCos && distance < bestDistance)) {
                best = candidate;
                bestCos = cos;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    private static double length(Vec3 vector) {
        return Math.sqrt(vector.x() * vector.x() + vector.y() * vector.y() + vector.z() * vector.z());
    }
}
