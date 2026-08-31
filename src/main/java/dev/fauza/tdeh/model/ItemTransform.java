package dev.fauza.tdeh.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Mirrors {@code org.bukkit.entity.ItemDisplay.ItemDisplayTransform}. The transform decides which
 * vanilla item model variant is rendered, which visibly changes the shape of tools and blocks.
 */
public enum ItemTransform {
    NONE,
    THIRDPERSON_LEFTHAND,
    THIRDPERSON_RIGHTHAND,
    FIRSTPERSON_LEFTHAND,
    FIRSTPERSON_RIGHTHAND,
    HEAD,
    GUI,
    GROUND,
    FIXED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<ItemTransform> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (ItemTransform transform : values()) {
            if (transform.name().equalsIgnoreCase(id)) {
                return Optional.of(transform);
            }
        }
        return Optional.empty();
    }
}
