package dev.fauza.tdeh.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFormatTest {

    @Test
    void miniMessageTagsAreRendered() {
        Component rendered = TextFormat.render("<red>danger");

        assertEquals(TextFormat.Dialect.MINI_MESSAGE, TextFormat.dialectOf("<red>danger"));
        assertEquals("danger", plain(rendered));
        assertEquals(NamedTextColor.RED, firstColor(rendered));
    }

    @Test
    void legacyAmpersandCodesAreConverted() {
        Component rendered = TextFormat.render("&6gold");

        assertEquals(TextFormat.Dialect.LEGACY, TextFormat.dialectOf("&6gold"));
        assertEquals("gold", plain(rendered));
        assertEquals(NamedTextColor.GOLD, firstColor(rendered));
    }

    @Test
    void legacyHexCodesAreConverted() {
        Component rendered = TextFormat.render("&#ff0055pink");

        assertEquals(TextFormat.Dialect.LEGACY, TextFormat.dialectOf("&#ff0055pink"));
        assertEquals("pink", plain(rendered));
        assertEquals(TextColor.color(0xFF0055), firstColor(rendered));
    }

    @Test
    void sectionSignCodesAreAlsoAccepted() {
        // Text pasted straight out of an older plugin's data file often already uses the section sign.
        assertEquals(TextFormat.Dialect.LEGACY, TextFormat.dialectOf("§cred"));
        assertEquals("red", plain(TextFormat.render("§cred")));
    }

    @Test
    void plainTextIsLeftAlone() {
        assertEquals(TextFormat.Dialect.PLAIN, TextFormat.dialectOf("just words"));
        assertEquals("just words", plain(TextFormat.render("just words")));
    }

    @Test
    void proseWithAngleBracketsIsNotMistakenForMarkup() {
        // The tag pattern is deliberately strict so arithmetic in a hologram does not blow up.
        assertEquals(TextFormat.Dialect.PLAIN, TextFormat.dialectOf("5 < 10 > 2"));
        assertEquals("5 < 10 > 2", plain(TextFormat.render("5 < 10 > 2")));
    }

    @Test
    void miniMessageWinsOverLegacyInMixedInput() {
        // Otherwise the legacy pass would eat the string and the gradient tags would be shown raw.
        String mixed = "<gradient:#ff0055:#ffaa00>HUB</gradient> &7| plain";

        assertEquals(TextFormat.Dialect.MINI_MESSAGE, TextFormat.dialectOf(mixed));
        assertTrue(plain(TextFormat.render(mixed)).startsWith("HUB"));
    }

    @Test
    void escapedNewlineBecomesARealLineBreak() {
        // Chat cannot carry a literal newline, so multi-line holograms rely on this escape.
        assertEquals("line one\nline two", plain(TextFormat.render("line one\\nline two")));
        assertEquals("a\nb", plain(TextFormat.render("&fa\\n&fb")));
    }

    @Test
    void nullAndEmptyInputRenderAsEmpty() {
        assertEquals("", plain(TextFormat.render(null)));
        assertEquals("", plain(TextFormat.render("")));
        assertEquals(TextFormat.Dialect.PLAIN, TextFormat.dialectOf(null));
    }

    @Test
    void plainStripsEveryDialect() {
        assertEquals("HUB", TextFormat.plain("<gold><bold>HUB</bold></gold>"));
        assertEquals("HUB", TextFormat.plain("&6&lHUB"));
        assertEquals("HUB", TextFormat.plain("HUB"));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static TextColor firstColor(Component component) {
        if (component.color() != null) {
            return component.color();
        }
        return component.children().stream()
                .map(TextFormatTest::firstColor)
                .filter(color -> color != null)
                .findFirst()
                .orElse(null);
    }
}
