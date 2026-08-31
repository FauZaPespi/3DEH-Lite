package dev.fauza.tdeh.manager;

import dev.fauza.tdeh.codec.HologramCodec;
import dev.fauza.tdeh.model.Hologram;
import dev.fauza.tdeh.model.HologramLocation;
import dev.fauza.tdeh.model.HologramName;
import dev.fauza.tdeh.model.HologramPart;
import dev.fauza.tdeh.render.DisplayRenderer;
import dev.fauza.tdeh.render.EntityTagger;
import dev.fauza.tdeh.store.HologramStore;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the hologram registry, the chunk index, and the live display entities.
 *
 * <h2>Threading</h2>
 * On Folia the registry is read from many region threads at once, so every collection here is
 * concurrent. Two invariants keep that safe without a global lock:
 * <ul>
 *   <li>All parts of a hologram share one anchor, hence one region. Every spawn and despawn for a
 *       given hologram therefore runs on that single region's thread, whether it came from a command
 *       or from a chunk event, and cannot race with itself.</li>
 *   <li>Model objects are mutable, and the autosave encodes them off the tick loop. Mutation and
 *       encoding both synchronise on the {@link Hologram} instance so a snapshot never catches a
 *       half-applied edit.</li>
 * </ul>
 */
public final class HologramManager {

    /** Identifies a chunk across worlds, for the "which holograms live here" index. */
    private record ChunkRef(String world, long key) {
    }

    private final Server server;
    private final Logger logger;
    private final Scheduling scheduling;
    private final DisplayRenderer renderer;
    private final EntityTagger tagger;
    private final HologramStore store;

    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private final Map<ChunkRef, Set<String>> byChunk = new ConcurrentHashMap<>();
    private final Map<String, List<Display>> spawned = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean();

    public HologramManager(Server server,
                           Logger logger,
                           Scheduling scheduling,
                           DisplayRenderer renderer,
                           EntityTagger tagger,
                           HologramStore store) {
        this.server = server;
        this.logger = logger;
        this.scheduling = scheduling;
        this.renderer = renderer;
        this.tagger = tagger;
        this.store = store;
    }

    // ---------------------------------------------------------------- registry

    public Optional<Hologram> find(String name) {
        return HologramName.isValid(name)
                ? Optional.ofNullable(holograms.get(HologramName.canonical(name)))
                : Optional.empty();
    }

    public boolean exists(String name) {
        return find(name).isPresent();
    }

    public Collection<Hologram> all() {
        return holograms.values();
    }

    public Set<String> names() {
        return holograms.keySet();
    }

    public int count() {
        return holograms.size();
    }

    // ---------------------------------------------------------------- mutation

    public void create(Hologram hologram) {
        holograms.put(hologram.name(), hologram);
        index(hologram);
        markDirty();
        spawnIfChunkLoaded(hologram);
    }

    public void remove(Hologram hologram) {
        holograms.remove(hologram.name());
        unindex(hologram);
        markDirty();
        despawn(hologram.name());
    }

    /**
     * Applies an edit to one part and pushes it to the live entity without respawning, so the change
     * is seamless for anyone already looking at the hologram.
     */
    public void editPart(Hologram hologram, int partIndex, Consumer<HologramPart> edit) {
        HologramPart part = hologram.part(partIndex);
        synchronized (hologram) {
            edit.accept(part);
        }
        markDirty();

        List<Display> displays = spawned.get(hologram.name());
        if (displays == null || partIndex >= displays.size()) {
            return;
        }
        Display display = displays.get(partIndex);
        scheduling.forEntity(display, () -> {
            try {
                renderer.apply(display, part);
            } catch (RuntimeException cause) {
                logger.log(Level.WARNING,
                        "Could not update hologram '" + hologram.name() + "' part #" + partIndex, cause);
            }
            // The entity died between the edit and the task running; the chunk event will rebuild it.
        }, () -> despawn(hologram.name()));
    }

    /**
     * Applies a change that alters the shape of the hologram: the anchor, or the part list. Part
     * indices back the entity list positionally, so anything that shifts them has to respawn.
     */
    public void restructure(Hologram hologram, Runnable change) {
        despawn(hologram.name());
        unindex(hologram);
        synchronized (hologram) {
            change.run();
        }
        index(hologram);
        markDirty();
        spawnIfChunkLoaded(hologram);
    }

    // ---------------------------------------------------------------- entities

    private void spawnIfChunkLoaded(Hologram hologram) {
        World world = server.getWorld(hologram.location().world());
        if (world == null) {
            // The world is not loaded; the chunk listener will pick the hologram up if it ever is.
            return;
        }
        HologramLocation anchor = hologram.location();
        if (!world.isChunkLoaded(anchor.chunkX(), anchor.chunkZ())) {
            return;
        }
        scheduling.atLocation(toLocation(world, anchor), () -> spawnNow(hologram, world));
    }

    /** Must run on the thread owning the hologram's region. */
    private void spawnNow(Hologram hologram, World world) {
        if (spawned.containsKey(hologram.name())) {
            return;
        }
        List<Display> displays = new CopyOnWriteArrayList<>();
        try {
            for (int i = 0; i < hologram.partCount(); i++) {
                HologramPart part = hologram.part(i);
                Location location = toLocation(world, hologram.partLocation(i));
                displays.add(renderer.spawn(world, location, part, hologram.name(), i));
            }
        } catch (RuntimeException cause) {
            // A bad material or block state must not leave half a hologram behind.
            logger.log(Level.WARNING, "Could not spawn hologram '" + hologram.name() + "'", cause);
            displays.forEach(Display::remove);
            return;
        }
        spawned.put(hologram.name(), displays);
    }

    /** Removes the live entities of a hologram, leaving its definition in the registry. */
    public void despawn(String name) {
        List<Display> displays = spawned.remove(name);
        if (displays == null) {
            return;
        }
        for (Display display : displays) {
            scheduling.forEntity(display, display::remove, () -> {
                // Already gone, nothing to remove.
            });
        }
    }

    public boolean isSpawned(String name) {
        return spawned.containsKey(name);
    }

    /**
     * Called when a chunk's entities become available. Runs on the thread owning that chunk's region.
     *
     * <p>Any tagged entity found here is a leftover: parts are spawned non-persistent, so the server
     * never restores them itself. Sweeping them and respawning from the model is both the orphan
     * cleanup and the guarantee that what is rendered matches what is stored.
     */
    public void onChunkEntitiesLoaded(World world, int chunkX, int chunkZ, Collection<Entity> entities) {
        for (Entity entity : entities) {
            if (tagger.isTagged(entity)) {
                entity.remove();
            }
        }
        for (String name : hologramsIn(world.getName(), HologramLocation.chunkKey(chunkX, chunkZ))) {
            Hologram hologram = holograms.get(name);
            if (hologram != null) {
                spawned.remove(name);
                spawnNow(hologram, world);
            }
        }
    }

    /** Non-persistent entities are discarded with the chunk, so stop tracking them. */
    public void onChunkEntitiesUnloaded(World world, int chunkX, int chunkZ) {
        for (String name : hologramsIn(world.getName(), HologramLocation.chunkKey(chunkX, chunkZ))) {
            spawned.remove(name);
        }
    }

    /** Despawns everything. Used on shutdown and before a reload. */
    public void despawnAll() {
        for (String name : List.copyOf(spawned.keySet())) {
            despawn(name);
        }
    }

    // ---------------------------------------------------------------- persistence

    /** Replaces the registry with what is on disk and respawns whatever is in a loaded chunk. */
    public int reload() {
        despawnAll();
        holograms.clear();
        byChunk.clear();

        Map<String, Hologram> loaded = store.load();
        holograms.putAll(loaded);
        loaded.values().forEach(this::index);
        loaded.values().forEach(this::spawnIfChunkLoaded);
        dirty.set(false);
        return loaded.size();
    }

    public void markDirty() {
        dirty.set(true);
    }

    /**
     * Writes to disk on the calling thread if anything changed since the last write. Must not be
     * called from a region thread; use {@link #requestSave()} from command handlers.
     *
     * <p>The compare-and-set also debounces: a burst of edits collapses into a single write, and
     * concurrent callers cannot both pick up the same pending change.
     */
    public void flush() {
        if (dirty.compareAndSet(true, false)) {
            store.save(snapshot());
        }
    }

    /** Queues a {@link #flush()} off the tick loop. The normal path after a command edits something. */
    public void requestSave() {
        scheduling.async(this::flush);
    }

    private Map<String, Map<String, Object>> snapshot() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Hologram hologram : holograms.values()) {
            synchronized (hologram) {
                out.put(hologram.name(), HologramCodec.encode(hologram));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- index

    private void index(Hologram hologram) {
        ChunkRef ref = refOf(hologram);
        byChunk.computeIfAbsent(ref, key -> ConcurrentHashMap.newKeySet()).add(hologram.name());
    }

    private void unindex(Hologram hologram) {
        ChunkRef ref = refOf(hologram);
        Set<String> names = byChunk.get(ref);
        if (names != null) {
            names.remove(hologram.name());
            if (names.isEmpty()) {
                byChunk.remove(ref, names);
            }
        }
    }

    private Collection<String> hologramsIn(String world, long chunkKey) {
        Set<String> names = byChunk.get(new ChunkRef(world, chunkKey));
        return names == null ? List.of() : new ArrayList<>(names);
    }

    private static ChunkRef refOf(Hologram hologram) {
        return new ChunkRef(hologram.location().world(), hologram.location().chunkKey());
    }

    public static Location toLocation(World world, HologramLocation location) {
        return new Location(world, location.x(), location.y(), location.z());
    }
}
