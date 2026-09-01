package dev.fauza.tdeh.codec;

import dev.fauza.tdeh.model.BillboardMode;
import dev.fauza.tdeh.model.BlockPart;
import dev.fauza.tdeh.model.Brightness;
import dev.fauza.tdeh.model.EulerRotation;
import dev.fauza.tdeh.model.Hologram;
import dev.fauza.tdeh.model.HologramLocation;
import dev.fauza.tdeh.model.ItemPart;
import dev.fauza.tdeh.model.ItemTransform;
import dev.fauza.tdeh.model.TextAlign;
import dev.fauza.tdeh.model.TextPart;
import dev.fauza.tdeh.model.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HologramCodecTest {

    private static final HologramLocation SPAWN = new HologramLocation("world", 0.5, 65.0, 12.5);

    @Test
    void encodeOmitsDefaultsSoHandEditedFilesStaySmall() {
        Hologram hologram = new Hologram("welcome", SPAWN, new TextPart("&6Hello"));

        Map<String, Object> encoded = HologramCodec.encode(hologram);
        Map<?, ?> part = (Map<?, ?>) ((List<?>) encoded.get("parts")).get(0);

        assertEquals(Map.of("type", "text", "text", "&6Hello"), part);
    }

    @Test
    void encodeKeepsOnlyTheAttributesThatWereChanged() {
        TextPart text = new TextPart("hi");
        text.billboard(BillboardMode.VERTICAL);
        text.scale(Vec3.of(2));

        Map<?, ?> part = firstPart(new Hologram("h", SPAWN, text));

        assertEquals("VERTICAL", part.get("billboard"));
        assertEquals(List.of(2.0, 2.0, 2.0), part.get("scale"));
        assertFalse(part.containsKey("view-range"), "unchanged attribute must not be written");
        assertFalse(part.containsKey("shadow-radius"), "unchanged attribute must not be written");
    }

    @Test
    void textPartSurvivesARoundTrip() {
        TextPart text = new TextPart("<red>danger");
        text.offset(new Vec3(0, 1.5, 0));
        text.scale(Vec3.of(1.25));
        text.translation(new Vec3(0.1, 0.2, 0.3));
        text.rotation(new EulerRotation(90, 45, 15));
        text.billboard(BillboardMode.CENTER);
        text.viewRange(2.5f);
        text.shadowRadius(0.5f);
        text.shadowStrength(0.75f);
        text.displayWidth(3f);
        text.displayHeight(4f);
        text.interpolationDelay(2);
        text.interpolationDuration(10);
        text.teleportDuration(20);
        text.glowColor(0xFF0055);
        text.brightness(new Brightness(15, 12));
        text.lineWidth(120);
        text.opacity(200);
        text.textShadow(true);
        text.seeThrough(true);
        text.backgroundColor(0x80112233);
        text.alignment(TextAlign.LEFT);

        TextPart decoded = assertInstanceOf(TextPart.class, roundTrip(new Hologram("h", SPAWN, text)).part(0));

        assertEquals("<red>danger", decoded.text());
        assertEquals(new Vec3(0, 1.5, 0), decoded.offset());
        assertEquals(Vec3.of(1.25), decoded.scale());
        assertEquals(new Vec3(0.1, 0.2, 0.3), decoded.translation());
        assertEquals(new EulerRotation(90, 45, 15), decoded.rotation());
        assertEquals(BillboardMode.CENTER, decoded.billboard(), "an opted-in mode survives a reload");
        assertEquals(2.5f, decoded.viewRange());
        assertEquals(0.5f, decoded.shadowRadius());
        assertEquals(0.75f, decoded.shadowStrength());
        assertEquals(3f, decoded.displayWidth());
        assertEquals(4f, decoded.displayHeight());
        assertEquals(2, decoded.interpolationDelay());
        assertEquals(10, decoded.interpolationDuration());
        assertEquals(20, decoded.teleportDuration());
        assertEquals(0xFF0055, decoded.glowColor());
        assertEquals(new Brightness(15, 12), decoded.brightness());
        assertEquals(120, decoded.lineWidth());
        assertEquals(200, decoded.opacity());
        assertTrue(decoded.textShadow());
        assertTrue(decoded.seeThrough());
        assertEquals(0x80112233, decoded.backgroundColor());
        assertFalse(decoded.defaultBackground(), "an explicit background colour disables the default box");
        assertEquals(TextAlign.LEFT, decoded.alignment());
    }

    @Test
    void blockAndItemPartsSurviveARoundTrip() {
        ItemPart item = new ItemPart("diamond_sword");
        item.transform(ItemTransform.HEAD);
        item.offset(new Vec3(0, 2, 0));
        Hologram hologram = new Hologram("shop", SPAWN,
                List.of(new BlockPart("minecraft:oak_log[axis=y]"), item));

        Hologram decoded = roundTrip(hologram);

        assertEquals(2, decoded.partCount());
        assertEquals("minecraft:oak_log[axis=y]",
                assertInstanceOf(BlockPart.class, decoded.part(0)).blockData());
        ItemPart decodedItem = assertInstanceOf(ItemPart.class, decoded.part(1));
        assertEquals("DIAMOND_SWORD", decodedItem.material(), "materials are normalised to upper case");
        assertEquals(ItemTransform.HEAD, decodedItem.transform());
        assertEquals(new Vec3(0, 2, 0), decodedItem.offset());
    }

    @Test
    void multiPartOrderIsPreserved() {
        Hologram hologram = new Hologram("stack", SPAWN,
                List.of(new TextPart("one"), new TextPart("two"), new TextPart("three")));

        Hologram decoded = roundTrip(hologram);

        assertEquals(List.of("one", "two", "three"), decoded.parts().stream()
                .map(part -> ((TextPart) part).text())
                .toList());
    }

    @Test
    void missingOptionalKeysFallBackToDefaults() {
        Hologram decoded = HologramCodec.decode("h", Map.of(
                "world", "world", "x", 1.0, "y", 2.0, "z", 3.0,
                "parts", List.of(Map.of("type", "text", "text", "hi"))));

        TextPart part = assertInstanceOf(TextPart.class, decoded.part(0));
        assertEquals(Vec3.ZERO, part.offset());
        assertEquals(Vec3.ONE, part.scale());
        assertEquals(BillboardMode.FIXED, part.billboard(), "billboard tracking is opt-in");
        assertEquals(1.0f, part.viewRange());
        assertEquals(200, part.lineWidth());
        assertEquals(TextPart.DEFAULT_OPACITY, part.opacity());
        assertTrue(part.defaultBackground());
        assertNull(part.glowColor());
        assertNull(part.brightness());
    }

    @Test
    void yamlIntegersAreAcceptedWhereDoublesAreExpected() {
        // SnakeYAML hands back Integer for "y: 65", so the codec must widen rather than fail.
        Hologram decoded = HologramCodec.decode("h", Map.of(
                "world", "world", "x", 1, "y", 65, "z", 3,
                "parts", List.of(Map.of("type", "text", "text", "hi", "view-range", 2))));

        assertEquals(65.0, decoded.location().y());
        assertEquals(2.0f, decoded.part(0).viewRange());
    }

    @Test
    void missingRequiredKeyIsReportedByName() {
        CodecException error = assertThrows(CodecException.class, () -> HologramCodec.decode("h", Map.of(
                "x", 1.0, "y", 2.0, "z", 3.0,
                "parts", List.of(Map.of("type", "text", "text", "hi")))));

        assertTrue(error.getMessage().contains("world"), error.getMessage());
    }

    @Test
    void malformedPartIsReportedWithItsIndex() {
        CodecException error = assertThrows(CodecException.class, () -> HologramCodec.decode("shop", Map.of(
                "world", "world", "x", 1.0, "y", 2.0, "z", 3.0,
                "parts", List.of(
                        Map.of("type", "text", "text", "ok"),
                        Map.of("type", "text", "text", "bad", "billboard", "SIDEWAYS")))));

        assertTrue(error.getMessage().contains("part #1"), error.getMessage());
        assertTrue(error.getMessage().contains("SIDEWAYS"), error.getMessage());
    }

    @Test
    void outOfRangeValuesFromAHandEditedFileAreRejected() {
        assertThrows(CodecException.class, () -> HologramCodec.decode("h", Map.of(
                "world", "world", "x", 1.0, "y", 2.0, "z", 3.0,
                "parts", List.of(Map.of("type", "text", "text", "hi", "brightness", List.of(99, 0))))));

        assertThrows(CodecException.class, () -> HologramCodec.decode("h", Map.of(
                "world", "world", "x", 1.0, "y", 2.0, "z", 3.0,
                "parts", List.of(Map.of("type", "text", "text", "hi", "teleport-duration", 120)))));
    }

    @Test
    void unknownPartTypeIsRejected() {
        CodecException error = assertThrows(CodecException.class, () -> HologramCodec.decode("h", Map.of(
                "world", "world", "x", 1.0, "y", 2.0, "z", 3.0,
                "parts", List.of(Map.of("type", "particle")))));

        assertTrue(error.getMessage().contains("particle"), error.getMessage());
    }

    @Test
    void hologramWithoutPartsIsRejected() {
        assertThrows(CodecException.class, () -> HologramCodec.decode("h", Map.of(
                "world", "world", "x", 1.0, "y", 2.0, "z", 3.0,
                "parts", List.of())));
    }

    private static Map<?, ?> firstPart(Hologram hologram) {
        return (Map<?, ?>) ((List<?>) HologramCodec.encode(hologram).get("parts")).get(0);
    }

    private static Hologram roundTrip(Hologram hologram) {
        return HologramCodec.decode(hologram.name(), HologramCodec.encode(hologram));
    }
}
