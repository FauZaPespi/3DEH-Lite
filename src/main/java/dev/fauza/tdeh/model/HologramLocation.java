package dev.fauza.tdeh.model;

import java.util.Objects;

/**
 * Where a hologram is anchored, as a world name plus coordinates. Deliberately not a Bukkit
 * {@code Location}: the manager indexes and serialises holograms from threads that may not own the
 * region, and a Bukkit Location holds a live World reference that must not travel that way.
 */
public record HologramLocation(String world, double x, double y, double z) {

    public HologramLocation {
        Objects.requireNonNull(world, "world");
        if (world.isBlank()) {
            throw new IllegalArgumentException("World name must not be blank");
        }
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public int chunkX() {
        return Math.floorDiv((int) Math.floor(x), 16);
    }

    public int chunkZ() {
        return Math.floorDiv((int) Math.floor(z), 16);
    }

    /**
     * Packs the chunk coordinates the same way {@code org.bukkit.Chunk#getChunkKey} does, so the
     * manager's index can be looked up directly from a Bukkit chunk without converting.
     */
    public long chunkKey() {
        return chunkKey(chunkX(), chunkZ());
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    private static void requireFinite(double value, String component) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Coordinate " + component + " must be finite, got " + value);
        }
    }
}
