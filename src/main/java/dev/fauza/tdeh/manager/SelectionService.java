package dev.fauza.tdeh.manager;

import dev.fauza.tdeh.math.LookSelector;
import dev.fauza.tdeh.model.Hologram;
import dev.fauza.tdeh.model.Vec3;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which hologram each player has selected, so attribute commands can be typed without
 * repeating the name.
 *
 * <p>Selections are per-player and live only for the session; they are dropped on quit so a
 * long-running server does not accumulate entries for players who never come back.
 */
public final class SelectionService {

    private final Map<UUID, String> selections = new ConcurrentHashMap<>();
    private final HologramManager manager;

    public SelectionService(HologramManager manager) {
        this.manager = manager;
    }

    /**
     * Resolves the hologram a player is looking at.
     *
     * <p>Only holograms in the player's own world are considered, and the geometry itself lives in
     * {@link LookSelector} so it stays unit-testable.
     */
    public Optional<Hologram> lookingAt(Player player, double maxDistance, double maxAngleDegrees) {
        Location eye = player.getEyeLocation();
        String world = player.getWorld().getName();

        List<LookSelector.Candidate> candidates = new ArrayList<>();
        for (Hologram hologram : manager.all()) {
            if (!hologram.location().world().equals(world)) {
                continue;
            }
            candidates.add(new LookSelector.Candidate(hologram.name(), centreOf(hologram)));
        }

        Vector direction = eye.getDirection();
        return LookSelector.select(
                        new Vec3(eye.getX(), eye.getY(), eye.getZ()),
                        new Vec3(direction.getX(), direction.getY(), direction.getZ()),
                        candidates,
                        maxDistance,
                        maxAngleDegrees)
                .flatMap(candidate -> manager.find(candidate.name()));
    }

    /**
     * Aims at the middle of the hologram rather than its anchor: a tall multi-part hologram anchored
     * at its base would otherwise force the player to look at their own feet to select it.
     */
    private static Vec3 centreOf(Hologram hologram) {
        double sumY = 0;
        for (int i = 0; i < hologram.partCount(); i++) {
            sumY += hologram.part(i).offset().y();
        }
        double averageOffsetY = sumY / hologram.partCount();
        return new Vec3(
                hologram.location().x(),
                hologram.location().y() + averageOffsetY,
                hologram.location().z());
    }

    public void select(Player player, Hologram hologram) {
        selections.put(player.getUniqueId(), hologram.name());
    }

    public void clear(Player player) {
        selections.remove(player.getUniqueId());
    }

    public void forget(UUID playerId) {
        selections.remove(playerId);
    }

    /**
     * @return the player's selection, or empty if they have none or it has since been deleted. A
     *         stale entry is dropped rather than reported, so deleting a hologram silently clears it
     *         for everyone who had it selected.
     */
    public Optional<Hologram> selected(Player player) {
        String name = selections.get(player.getUniqueId());
        if (name == null) {
            return Optional.empty();
        }
        Optional<Hologram> hologram = manager.find(name);
        if (hologram.isEmpty()) {
            selections.remove(player.getUniqueId());
        }
        return hologram;
    }
}
