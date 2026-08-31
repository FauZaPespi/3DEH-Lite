package dev.fauza.tdeh.text;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorCodecTest {

    @Test
    void hexWithAndWithoutHashAreEquivalent() {
        assertEquals(Optional.of(0xFF0055), ColorCodec.parse("#ff0055"));
        assertEquals(Optional.of(0xFF0055), ColorCodec.parse("ff0055"));
        assertEquals(Optional.of(0xFF0055), ColorCodec.parse("#FF0055"));
    }

    @Test
    void vanillaColourNamesAreAccepted() {
        assertEquals(Optional.of(0xFF5555), ColorCodec.parse("red"));
        assertEquals(Optional.of(0xFF5555), ColorCodec.parse("RED"));
        assertEquals(Optional.of(0x55FF55), ColorCodec.parse("green"));
    }

    @Test
    void nonColoursAreRejected() {
        assertTrue(ColorCodec.parse("mauve").isEmpty());
        assertTrue(ColorCodec.parse("#ff00").isEmpty());
        assertTrue(ColorCodec.parse("#gggggg").isEmpty());
        assertTrue(ColorCodec.parse("").isEmpty());
        assertTrue(ColorCodec.parse(null).isEmpty());
    }

    @Test
    void noneClearsRatherThanSets() {
        assertTrue(ColorCodec.isNone("none"));
        assertTrue(ColorCodec.isNone("NONE"));
        assertFalse(ColorCodec.isNone("red"));
        assertTrue(ColorCodec.parse("none").isEmpty());
    }

    @Test
    void formatRoundTripsThroughParse() {
        assertEquals("#ff0055", ColorCodec.format(0xFF0055));
        assertEquals("#000000", ColorCodec.format(0x000000));
        assertEquals(Optional.of(0x1A2B3C), ColorCodec.parse(ColorCodec.format(0x1A2B3C)));
    }

    @Test
    void argbKeepsItsAlphaChannel() {
        assertEquals(Optional.of(0x80112233), ColorCodec.parseArgb("#80112233"));
        assertEquals("#80112233", ColorCodec.formatArgb(0x80112233));
    }

    @Test
    void argbTreatsASixDigitHexAsFullyOpaque() {
        assertEquals(Optional.of(0xFFFF0055), ColorCodec.parseArgb("#ff0055"));
        assertEquals(Optional.of(0xFFFF5555), ColorCodec.parseArgb("red"));
    }

    @Test
    void argbFormatRoundTripsThroughParse() {
        assertEquals(Optional.of(0x40AABBCC), ColorCodec.parseArgb(ColorCodec.formatArgb(0x40AABBCC)));
    }
}
