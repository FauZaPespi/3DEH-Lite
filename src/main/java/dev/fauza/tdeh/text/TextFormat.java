package dev.fauza.tdeh.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Pattern;

/**
 * Turns the raw string an admin typed into a rendered component.
 *
 * <p>Three input dialects are accepted so text pasted from an older hologram plugin keeps working:
 * MiniMessage tags, legacy {@code &} codes (including {@code &#rrggbb} and the BungeeCord
 * {@code &x&r&r&g&g&b&b} form), and plain text. The dialect is detected rather than configured,
 * because mixing holograms from several sources in one server is the normal case.
 *
 * <p>Detection order matters: a string is only treated as legacy when it has no MiniMessage tag,
 * otherwise {@code <gradient:#a:#b>&f</gradient>} would lose its tags.
 */
public final class TextFormat {

    /** The section sign, which arrives in text copied out of older plugins' data files. */
    private static final char SECTION_SIGN = '§';

    private static final char AMPERSAND = '&';

    /**
     * A plausible MiniMessage tag: an opening angle bracket followed by a tag-ish name. Deliberately
     * strict so ordinary prose such as {@code "5 < 10"} is not mistaken for markup.
     */
    private static final Pattern MINI_TAG = Pattern.compile("<[a-zA-Z#/][^<>]*>");

    private static final Pattern LEGACY_CODE = Pattern.compile("[&§]");

    /** Two-character escape for a line break, since a real newline cannot be typed into chat. */
    private static final String NEWLINE_ESCAPE = "\\n";

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(AMPERSAND)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private TextFormat() {
    }

    /** Renders raw admin input, resolving the escape for line breaks first. */
    public static Component render(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        String normalised = normalise(raw);
        return switch (dialectOfNormalised(normalised)) {
            case MINI_MESSAGE -> MINI_MESSAGE.deserialize(normalised);
            // The serializer is built around '&', so fold the section sign onto it rather than
            // maintaining a second serializer that differs only by its marker character.
            case LEGACY -> LEGACY.deserialize(normalised.replace(SECTION_SIGN, AMPERSAND));
            case PLAIN -> Component.text(normalised);
        };
    }

    /** Strips all formatting, for log lines and the compact {@code /3deh list} preview. */
    public static String plain(String raw) {
        return PlainTextComponentSerializer.plainText().serialize(render(raw));
    }

    /** Which dialect {@link #render} would use. Exposed for tests and {@code /3deh info}. */
    public static Dialect dialectOf(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Dialect.PLAIN;
        }
        return dialectOfNormalised(normalise(raw));
    }

    private static String normalise(String raw) {
        return raw.replace(NEWLINE_ESCAPE, "\n");
    }

    private static Dialect dialectOfNormalised(String normalised) {
        if (MINI_TAG.matcher(normalised).find()) {
            return Dialect.MINI_MESSAGE;
        }
        if (LEGACY_CODE.matcher(normalised).find()) {
            return Dialect.LEGACY;
        }
        return Dialect.PLAIN;
    }

    public enum Dialect {
        MINI_MESSAGE,
        LEGACY,
        PLAIN
    }
}
