package dev.fauza.tdeh.model;

import java.util.Locale;
import java.util.Optional;

/** Mirrors {@code org.bukkit.entity.TextDisplay.TextAlignment}. */
public enum TextAlign {
    CENTER,
    LEFT,
    RIGHT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<TextAlign> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (TextAlign align : values()) {
            if (align.name().equalsIgnoreCase(id)) {
                return Optional.of(align);
            }
        }
        return Optional.empty();
    }
}
