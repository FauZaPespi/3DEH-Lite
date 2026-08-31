package dev.fauza.tdeh.render;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Stamps spawned display entities with the hologram they belong to.
 *
 * <p>Parts are spawned non-persistent, so the server never writes them to a region file and a crash
 * cannot leave ghosts behind. The tag still matters for two cases the non-persistence trick does not
 * cover: re-adopting entities that are still alive after a {@code /reload}, and recognising leftovers
 * from an earlier install so {@code EntitiesLoadEvent} can sweep them up.
 */
public final class EntityTagger {

    private final NamespacedKey hologramKey;
    private final NamespacedKey partKey;

    public EntityTagger(Plugin plugin) {
        this.hologramKey = new NamespacedKey(plugin, "hologram");
        this.partKey = new NamespacedKey(plugin, "part");
    }

    public void tag(Display display, String hologramName, int partIndex) {
        PersistentDataContainer container = display.getPersistentDataContainer();
        container.set(hologramKey, PersistentDataType.STRING, hologramName);
        container.set(partKey, PersistentDataType.INTEGER, partIndex);
    }

    /** @return the hologram name stamped on the entity, or empty when it is not one of ours. */
    public Optional<String> hologramOf(Entity entity) {
        return Optional.ofNullable(
                entity.getPersistentDataContainer().get(hologramKey, PersistentDataType.STRING));
    }

    public int partIndexOf(Entity entity) {
        Integer index = entity.getPersistentDataContainer().get(partKey, PersistentDataType.INTEGER);
        return index == null ? -1 : index;
    }

    public boolean isTagged(Entity entity) {
        return entity.getPersistentDataContainer().has(hologramKey, PersistentDataType.STRING);
    }
}
