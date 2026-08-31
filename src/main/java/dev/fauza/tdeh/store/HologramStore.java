package dev.fauza.tdeh.store;

import dev.fauza.tdeh.model.Hologram;

import java.util.Map;

/**
 * Persistence for the hologram definitions.
 *
 * <p>{@link #save} takes an already-encoded snapshot rather than the live holograms: it runs off the
 * tick loop, and handing it mutable model objects would mean serialising state that another region
 * thread is concurrently editing.
 */
public interface HologramStore {

    /** @return every hologram that could be read, keyed by canonical name. Unreadable entries are skipped. */
    Map<String, Hologram> load();

    void save(Map<String, Map<String, Object>> snapshot);
}
