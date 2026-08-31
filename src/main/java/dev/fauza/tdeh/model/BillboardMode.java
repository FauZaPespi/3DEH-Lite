package dev.fauza.tdeh.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Mirrors {@code org.bukkit.entity.Display.Billboard}. Duplicated rather than reused so the
 * model stays Bukkit-free; the renderer maps the two with an exhaustive switch, which turns any
 * future divergence into a compile error instead of a runtime one.
 */
public enum BillboardMode {
    FIXED,
    VERTICAL,
    HORIZONTAL,
    CENTER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<BillboardMode> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (BillboardMode mode : values()) {
            if (mode.name().equalsIgnoreCase(id)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
