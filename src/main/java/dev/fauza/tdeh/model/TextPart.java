package dev.fauza.tdeh.model;

import java.util.Objects;

/**
 * A {@code TextDisplay} part.
 *
 * <p>The raw, admin-typed string is what gets stored and round-tripped, never a serialised
 * component: an operator editing {@code data.yml} should see the text they wrote, and the
 * MiniMessage/legacy rendering stays recomputable if the formatting rules ever change.
 */
public final class TextPart extends HologramPart {

    /** Vanilla treats -1 as "use the default opacity" rather than "fully transparent". */
    public static final int DEFAULT_OPACITY = -1;
    public static final int MIN_OPACITY = 0;
    public static final int MAX_OPACITY = 255;

    private String text;
    private int lineWidth = 200;
    private int opacity = DEFAULT_OPACITY;
    private boolean textShadow;
    private boolean seeThrough;
    private boolean defaultBackground = true;
    private Integer backgroundColor;
    private TextAlign alignment = TextAlign.CENTER;

    public TextPart(String text) {
        super(DisplayType.TEXT);
        text(text);
    }

    public String text() {
        return text;
    }

    public void text(String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    public int lineWidth() {
        return lineWidth;
    }

    public void lineWidth(int lineWidth) {
        if (lineWidth <= 0) {
            throw new IllegalArgumentException("Line width must be > 0, got " + lineWidth);
        }
        this.lineWidth = lineWidth;
    }

    public int opacity() {
        return opacity;
    }

    public void opacity(int opacity) {
        if (opacity != DEFAULT_OPACITY && (opacity < MIN_OPACITY || opacity > MAX_OPACITY)) {
            throw new IllegalArgumentException("Opacity must be " + DEFAULT_OPACITY + " (default) or "
                    + MIN_OPACITY + "-" + MAX_OPACITY + ", got " + opacity);
        }
        this.opacity = opacity;
    }

    public boolean textShadow() {
        return textShadow;
    }

    public void textShadow(boolean textShadow) {
        this.textShadow = textShadow;
    }

    public boolean seeThrough() {
        return seeThrough;
    }

    public void seeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
    }

    public boolean defaultBackground() {
        return defaultBackground;
    }

    public void defaultBackground(boolean defaultBackground) {
        this.defaultBackground = defaultBackground;
    }

    /** @return packed 0xAARRGGBB, or {@code null} when no explicit background colour is set. */
    public Integer backgroundColor() {
        return backgroundColor;
    }

    /**
     * Setting an explicit colour turns the default background off, otherwise vanilla keeps drawing
     * the translucent grey box and the colour appears to be ignored.
     */
    public void backgroundColor(Integer argb) {
        this.backgroundColor = argb;
        if (argb != null) {
            this.defaultBackground = false;
        }
    }

    public TextAlign alignment() {
        return alignment;
    }

    public void alignment(TextAlign alignment) {
        this.alignment = Objects.requireNonNull(alignment, "alignment");
    }
}
