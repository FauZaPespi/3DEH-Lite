package dev.fauza.tdeh.listener;

import dev.fauza.tdeh.manager.HologramManager;
import dev.fauza.tdeh.manager.SelectionService;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

/**
 * Keeps the live entities in step with chunk and world loading.
 *
 * <p>{@code EntitiesLoadEvent} is used rather than {@code ChunkLoadEvent} for two reasons: it is the
 * point at which a chunk's entities are actually reachable, and on Folia it is already dispatched on
 * the thread owning that chunk's region, so the handler can touch entities directly.
 */
public final class ChunkLifecycleListener implements Listener {

    private final HologramManager manager;
    private final SelectionService selections;

    public ChunkLifecycleListener(HologramManager manager, SelectionService selections) {
        this.manager = manager;
        this.selections = selections;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        Chunk chunk = event.getChunk();
        manager.onChunkEntitiesLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ(), event.getEntities());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        Chunk chunk = event.getChunk();
        manager.onChunkEntitiesUnloaded(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selections.forget(event.getPlayer().getUniqueId());
    }
}
