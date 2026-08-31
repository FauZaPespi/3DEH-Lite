package dev.fauza.tdeh.model;

import java.util.Objects;

/**
 * One display entity inside a hologram, carrying the attributes shared by all three display
 * flavours. Subclasses add the type-specific ones.
 *
 * <p>Setters validate their own bounds rather than trusting callers. Both entry points into the
 * model are untrusted: Brigadier enforces ranges for typed command arguments, but a hand-edited
 * {@code data.yml} bypasses that entirely, so the invariants have to live here.
 */
public abstract class HologramPart {

    public static final int MIN_TELEPORT_DURATION = 0;
    public static final int MAX_TELEPORT_DURATION = 59;

    private final DisplayType type;

    private Vec3 offset = Vec3.ZERO;
    private Vec3 scale = Vec3.ONE;
    private Vec3 translation = Vec3.ZERO;
    private EulerRotation rotation = EulerRotation.NONE;
    private BillboardMode billboard = BillboardMode.CENTER;
    private float viewRange = 1.0f;
    private float shadowRadius;
    private float shadowStrength = 1.0f;
    private float displayWidth;
    private float displayHeight;
    private int interpolationDelay;
    private int interpolationDuration;
    private int teleportDuration;
    private Integer glowColor;
    private Brightness brightness;

    protected HologramPart(DisplayType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public final DisplayType type() {
        return type;
    }

    public Vec3 offset() {
        return offset;
    }

    public void offset(Vec3 offset) {
        this.offset = Objects.requireNonNull(offset, "offset");
    }

    public Vec3 scale() {
        return scale;
    }

    public void scale(Vec3 scale) {
        this.scale = Objects.requireNonNull(scale, "scale");
    }

    public Vec3 translation() {
        return translation;
    }

    public void translation(Vec3 translation) {
        this.translation = Objects.requireNonNull(translation, "translation");
    }

    public EulerRotation rotation() {
        return rotation;
    }

    public void rotation(EulerRotation rotation) {
        this.rotation = Objects.requireNonNull(rotation, "rotation");
    }

    public BillboardMode billboard() {
        return billboard;
    }

    public void billboard(BillboardMode billboard) {
        this.billboard = Objects.requireNonNull(billboard, "billboard");
    }

    public float viewRange() {
        return viewRange;
    }

    public void viewRange(float viewRange) {
        this.viewRange = requireNonNegative(viewRange, "view range");
    }

    public float shadowRadius() {
        return shadowRadius;
    }

    public void shadowRadius(float shadowRadius) {
        this.shadowRadius = requireNonNegative(shadowRadius, "shadow radius");
    }

    public float shadowStrength() {
        return shadowStrength;
    }

    public void shadowStrength(float shadowStrength) {
        this.shadowStrength = requireNonNegative(shadowStrength, "shadow strength");
    }

    public float displayWidth() {
        return displayWidth;
    }

    public void displayWidth(float displayWidth) {
        this.displayWidth = requireNonNegative(displayWidth, "width");
    }

    public float displayHeight() {
        return displayHeight;
    }

    public void displayHeight(float displayHeight) {
        this.displayHeight = requireNonNegative(displayHeight, "height");
    }

    public int interpolationDelay() {
        return interpolationDelay;
    }

    public void interpolationDelay(int ticks) {
        this.interpolationDelay = requireNonNegative(ticks, "interpolation delay");
    }

    public int interpolationDuration() {
        return interpolationDuration;
    }

    public void interpolationDuration(int ticks) {
        this.interpolationDuration = requireNonNegative(ticks, "interpolation duration");
    }

    public int teleportDuration() {
        return teleportDuration;
    }

    /** Vanilla rejects anything outside 0-59 with an exception, so reject it before it gets there. */
    public void teleportDuration(int ticks) {
        if (ticks < MIN_TELEPORT_DURATION || ticks > MAX_TELEPORT_DURATION) {
            throw new IllegalArgumentException("Teleport duration must be between " + MIN_TELEPORT_DURATION
                    + " and " + MAX_TELEPORT_DURATION + ", got " + ticks);
        }
        this.teleportDuration = ticks;
    }

    /** @return packed 0xRRGGBB, or {@code null} when the entity keeps its default glow colour. */
    public Integer glowColor() {
        return glowColor;
    }

    public void glowColor(Integer rgb) {
        if (rgb != null && (rgb < 0 || rgb > 0xFFFFFF)) {
            throw new IllegalArgumentException("Glow colour must be a 0xRRGGBB value, got " + rgb);
        }
        this.glowColor = rgb;
    }

    /** @return the light override, or {@code null} when the entity follows world lighting. */
    public Brightness brightness() {
        return brightness;
    }

    public void brightness(Brightness brightness) {
        this.brightness = brightness;
    }

    private static float requireNonNegative(float value, String label) {
        if (!Float.isFinite(value) || value < 0f) {
            throw new IllegalArgumentException(label + " must be a finite value >= 0, got " + value);
        }
        return value;
    }

    private static int requireNonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " must be >= 0, got " + value);
        }
        return value;
    }
}
