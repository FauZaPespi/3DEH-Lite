package dev.fauza.tdeh.model;

/**
 * Fixed light levels overriding the world lighting for a display entity.
 * Both channels are vanilla light levels, so 0-15 inclusive.
 */
public record Brightness(int block, int sky) {

    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 15;

    public Brightness {
        requireLevel(block, "block");
        requireLevel(sky, "sky");
    }

    private static void requireLevel(int level, String channel) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    channel + " brightness must be between " + MIN_LEVEL + " and " + MAX_LEVEL + ", got " + level);
        }
    }
}
