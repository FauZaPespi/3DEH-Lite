package dev.fauza.tdeh.model;

import java.util.Locale;
import java.util.Objects;

/**
 * An {@code ItemDisplay} part. The material is held by name and resolved against the server
 * registry at render time, so an unknown material degrades to a load warning instead of breaking
 * the whole data file.
 */
public final class ItemPart extends HologramPart {

    private String material;
    private ItemTransform transform = ItemTransform.FIXED;

    public ItemPart(String material) {
        super(DisplayType.ITEM);
        material(material);
    }

    public String material() {
        return material;
    }

    public void material(String material) {
        if (material == null || material.isBlank()) {
            throw new IllegalArgumentException("Material must not be blank");
        }
        this.material = material.trim().toUpperCase(Locale.ROOT);
    }

    public ItemTransform transform() {
        return transform;
    }

    public void transform(ItemTransform transform) {
        this.transform = Objects.requireNonNull(transform, "transform");
    }
}
