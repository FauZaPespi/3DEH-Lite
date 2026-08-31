package dev.fauza.tdeh.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A named group of display entity parts sharing one anchor location. Every part is positioned
 * relative to that anchor, so moving the hologram moves the whole group.
 *
 * <p>A hologram always holds at least one part; removing the last one is rejected, because an
 * empty hologram would be invisible yet still occupy a name.
 */
public final class Hologram {

    private final String name;
    private HologramLocation location;
    private final List<HologramPart> parts = new ArrayList<>();

    public Hologram(String name, HologramLocation location, List<HologramPart> parts) {
        this.name = HologramName.canonical(name);
        this.location = Objects.requireNonNull(location, "location");
        Objects.requireNonNull(parts, "parts");
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Hologram " + this.name + " must have at least one part");
        }
        this.parts.addAll(parts);
    }

    public Hologram(String name, HologramLocation location, HologramPart part) {
        this(name, location, List.of(part));
    }

    public String name() {
        return name;
    }

    public HologramLocation location() {
        return location;
    }

    public void location(HologramLocation location) {
        this.location = Objects.requireNonNull(location, "location");
    }

    public List<HologramPart> parts() {
        return Collections.unmodifiableList(parts);
    }

    public int partCount() {
        return parts.size();
    }

    public HologramPart part(int index) {
        if (index < 0 || index >= parts.size()) {
            throw new IndexOutOfBoundsException(
                    "Hologram " + name + " has no part #" + index + " (0-" + (parts.size() - 1) + ")");
        }
        return parts.get(index);
    }

    public int addPart(HologramPart part) {
        parts.add(Objects.requireNonNull(part, "part"));
        return parts.size() - 1;
    }

    public HologramPart removePart(int index) {
        if (parts.size() == 1) {
            throw new IllegalStateException("Cannot remove the last part of hologram " + name);
        }
        if (index < 0 || index >= parts.size()) {
            throw new IndexOutOfBoundsException(
                    "Hologram " + name + " has no part #" + index + " (0-" + (parts.size() - 1) + ")");
        }
        return parts.remove(index);
    }

    /** Absolute position of a part, i.e. the hologram anchor plus that part's offset. */
    public HologramLocation partLocation(int index) {
        Vec3 offset = part(index).offset();
        return new HologramLocation(location.world(),
                location.x() + offset.x(),
                location.y() + offset.y(),
                location.z() + offset.z());
    }
}
