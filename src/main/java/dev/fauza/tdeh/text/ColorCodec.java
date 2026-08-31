package dev.fauza.tdeh.text;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;
import java.util.Optional;

/**
 * Parses and formats the colour arguments used by the {@code glow} and {@code background}
 * attributes. Accepts {@code #rrggbb}, bare {@code rrggbb}, and the sixteen vanilla colour names,
 * so operators can type {@code red} as readily as a hex triplet.
 */
public final class ColorCodec {

    /** Argument that clears a colour override rather than setting one. */
    public static final String NONE = "none";

    private ColorCodec() {
    }

    /**
     * @return the packed 0xRRGGBB value, or empty when the input is not a colour. An input of
     *         {@code none} is a valid clear request and is also reported as empty, so callers that
     *         need to tell the two apart should check {@link #isNone(String)} first.
     */
    public static Optional<Integer> parse(String input) {
        if (input == null || input.isBlank() || isNone(input)) {
            return Optional.empty();
        }
        String value = input.trim();
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() == 6 && hex.chars().allMatch(ColorCodec::isHexDigit)) {
            return Optional.of(Integer.parseInt(hex, 16));
        }
        NamedTextColor named = NamedTextColor.NAMES.value(value.toLowerCase(Locale.ROOT));
        return named == null ? Optional.empty() : Optional.of(named.value());
    }

    public static boolean isNone(String input) {
        return input != null && NONE.equalsIgnoreCase(input.trim());
    }

    /** Formats a packed RGB value back into the {@code #rrggbb} form commands accept. */
    public static String format(int rgb) {
        return String.format("#%06x", rgb & 0xFFFFFF);
    }

    public static TextColor toTextColor(int rgb) {
        return TextColor.color(rgb & 0xFFFFFF);
    }

    /**
     * Parses a colour that may carry an alpha channel, as the text background does. Accepts
     * {@code #aarrggbb}, {@code #rrggbb} (treated as fully opaque) and the vanilla colour names.
     *
     * @return the packed 0xAARRGGBB value, or empty when the input is not a colour or is {@code none}
     */
    public static Optional<Integer> parseArgb(String input) {
        if (input == null || input.isBlank() || isNone(input)) {
            return Optional.empty();
        }
        String value = input.trim();
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() == 8 && hex.chars().allMatch(ColorCodec::isHexDigit)) {
            return Optional.of((int) Long.parseLong(hex, 16));
        }
        return parse(input).map(rgb -> 0xFF000000 | rgb);
    }

    /** Formats a packed ARGB value back into the {@code #aarrggbb} form commands accept. */
    public static String formatArgb(int argb) {
        return String.format("#%08x", argb);
    }

    private static boolean isHexDigit(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9')
                || (codePoint >= 'a' && codePoint <= 'f')
                || (codePoint >= 'A' && codePoint <= 'F');
    }
}
