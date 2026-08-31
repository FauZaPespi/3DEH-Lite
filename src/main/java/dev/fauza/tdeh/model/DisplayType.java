package dev.fauza.tdeh.model;

import java.util.Locale;
import java.util.Optional;

/**
 * The three display entity flavours 3DEH can render.
 */
public enum DisplayType {
    TEXT,
    BLOCK,
    ITEM;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<DisplayType> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (DisplayType type : values()) {
            if (type.id().equalsIgnoreCase(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
