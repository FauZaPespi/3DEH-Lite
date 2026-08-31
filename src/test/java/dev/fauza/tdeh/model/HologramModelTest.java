package dev.fauza.tdeh.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HologramModelTest {

    private static final HologramLocation SPAWN = new HologramLocation("world", 10.5, 65.0, -20.5);

    @Nested
    class Names {

        @Test
        void allowedCharactersAreAccepted() {
            assertTrue(HologramName.isValid("shop"));
            assertTrue(HologramName.isValid("Shop_2"));
            assertTrue(HologramName.isValid("spawn-info"));
            assertTrue(HologramName.isValid("a"));
        }

        @Test
        void charactersThatWouldBreakYamlKeysAreRejected() {
            assertFalse(HologramName.isValid("my hologram"));
            assertFalse(HologramName.isValid("shop.main"));
            assertFalse(HologramName.isValid("shop:main"));
            assertFalse(HologramName.isValid("café"));
            assertFalse(HologramName.isValid(""));
            assertFalse(HologramName.isValid(null));
        }

        @Test
        void namesLongerThanTheLimitAreRejected() {
            assertTrue(HologramName.isValid("a".repeat(HologramName.MAX_LENGTH)));
            assertFalse(HologramName.isValid("a".repeat(HologramName.MAX_LENGTH + 1)));
        }

        @Test
        void lookupsAreCaseInsensitive() {
            assertEquals("shop", HologramName.canonical("Shop"));
            assertEquals(HologramName.canonical("SHOP"), HologramName.canonical("shop"));
            assertThrows(IllegalArgumentException.class, () -> HologramName.canonical("bad name"));
        }
    }

    @Nested
    class Parts {

        @Test
        void teleportDurationIsClampedToTheVanillaRange() {
            TextPart part = new TextPart("hi");

            part.teleportDuration(0);
            part.teleportDuration(59);
            assertThrows(IllegalArgumentException.class, () -> part.teleportDuration(60));
            assertThrows(IllegalArgumentException.class, () -> part.teleportDuration(-1));
        }

        @Test
        void negativeSizesAreRejected() {
            TextPart part = new TextPart("hi");

            assertThrows(IllegalArgumentException.class, () -> part.viewRange(-1f));
            assertThrows(IllegalArgumentException.class, () -> part.shadowRadius(-0.1f));
            assertThrows(IllegalArgumentException.class, () -> part.displayWidth(-2f));
            assertThrows(IllegalArgumentException.class, () -> part.interpolationDuration(-1));
        }

        @Test
        void brightnessLevelsMustBeVanillaLightLevels() {
            new Brightness(0, 0);
            new Brightness(15, 15);
            assertThrows(IllegalArgumentException.class, () -> new Brightness(16, 0));
            assertThrows(IllegalArgumentException.class, () -> new Brightness(0, -1));
        }

        @Test
        void opacityAcceptsTheDefaultSentinelButNotOtherNegatives() {
            TextPart part = new TextPart("hi");

            part.opacity(TextPart.DEFAULT_OPACITY);
            part.opacity(0);
            part.opacity(255);
            assertThrows(IllegalArgumentException.class, () -> part.opacity(-2));
            assertThrows(IllegalArgumentException.class, () -> part.opacity(256));
        }

        @Test
        void lineWidthMustBePositive() {
            TextPart part = new TextPart("hi");

            assertThrows(IllegalArgumentException.class, () -> part.lineWidth(0));
            assertThrows(IllegalArgumentException.class, () -> part.lineWidth(-10));
        }

        @Test
        void glowColourMustFitInThreeBytes() {
            TextPart part = new TextPart("hi");

            part.glowColor(0xFFFFFF);
            part.glowColor(null);
            assertThrows(IllegalArgumentException.class, () -> part.glowColor(0x1000000));
            assertThrows(IllegalArgumentException.class, () -> part.glowColor(-1));
        }

        @Test
        void settingABackgroundColourTurnsOffTheDefaultBox() {
            TextPart part = new TextPart("hi");
            assertTrue(part.defaultBackground());

            part.backgroundColor(0x80112233);

            assertFalse(part.defaultBackground());
        }

        @Test
        void blankBlockDataAndMaterialsAreRejected() {
            assertThrows(IllegalArgumentException.class, () -> new BlockPart("  "));
            assertThrows(IllegalArgumentException.class, () -> new ItemPart(""));
        }

        @Test
        void materialsAreNormalisedToUpperCase() {
            assertEquals("DIAMOND_SWORD", new ItemPart(" diamond_sword ").material());
        }
    }

    @Nested
    class Holograms {

        @Test
        void aHologramMustKeepAtLeastOnePart() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Hologram("empty", SPAWN, List.of()));

            Hologram single = new Hologram("single", SPAWN, new TextPart("hi"));
            assertThrows(IllegalStateException.class, () -> single.removePart(0));
        }

        @Test
        void partsCanBeAddedAndRemovedByIndex() {
            Hologram hologram = new Hologram("shop", SPAWN, new TextPart("title"));

            int index = hologram.addPart(new ItemPart("diamond"));

            assertEquals(1, index);
            assertEquals(2, hologram.partCount());
            hologram.removePart(0);
            assertEquals(1, hologram.partCount());
            assertEquals(DisplayType.ITEM, hologram.part(0).type());
        }

        @Test
        void outOfRangePartIndexesAreReportedWithTheValidRange() {
            Hologram hologram = new Hologram("shop", SPAWN, new TextPart("title"));

            IndexOutOfBoundsException error =
                    assertThrows(IndexOutOfBoundsException.class, () -> hologram.part(3));

            assertTrue(error.getMessage().contains("#3"), error.getMessage());
        }

        @Test
        void partLocationAddsTheOffsetToTheAnchor() {
            TextPart part = new TextPart("hi");
            part.offset(new Vec3(1, 2.5, -3));
            Hologram hologram = new Hologram("shop", SPAWN, part);

            HologramLocation located = hologram.partLocation(0);

            assertEquals("world", located.world());
            assertEquals(11.5, located.x());
            assertEquals(67.5, located.y());
            assertEquals(-23.5, located.z());
        }

        @Test
        void theExposedPartListCannotBeMutatedFromOutside() {
            Hologram hologram = new Hologram("shop", SPAWN, new TextPart("title"));

            assertThrows(UnsupportedOperationException.class,
                    () -> hologram.parts().add(new TextPart("sneaky")));
        }

        @Test
        void movingTheHologramMovesEveryPartWithIt() {
            TextPart part = new TextPart("hi");
            part.offset(new Vec3(0, 2, 0));
            Hologram hologram = new Hologram("shop", SPAWN, part);

            hologram.location(new HologramLocation("nether", 0, 0, 0));

            assertEquals(new HologramLocation("nether", 0, 2, 0), hologram.partLocation(0));
        }
    }

    @Nested
    class Locations {

        @Test
        void chunkCoordinatesFloorTowardsNegativeInfinity() {
            assertEquals(0, new HologramLocation("world", 0.5, 0, 0.5).chunkX());
            assertEquals(0, new HologramLocation("world", 15.9, 0, 0).chunkX());
            assertEquals(1, new HologramLocation("world", 16.0, 0, 0).chunkX());
            assertEquals(-1, new HologramLocation("world", -0.5, 0, 0).chunkX());
            assertEquals(-2, new HologramLocation("world", -17.0, 0, 0).chunkX());
            assertEquals(-2, new HologramLocation("world", 0, 0, -17.0).chunkZ());
        }

        @Test
        void chunkKeyPacksBothCoordinatesReversibly() {
            HologramLocation location = new HologramLocation("world", -20.5, 65, 300.25);

            assertEquals(HologramLocation.chunkKey(location.chunkX(), location.chunkZ()), location.chunkKey());
            assertEquals(HologramLocation.chunkKey(-2, 18), location.chunkKey());
        }

        @Test
        void distinctChunksProduceDistinctKeys() {
            assertFalse(HologramLocation.chunkKey(1, 0) == HologramLocation.chunkKey(0, 1));
            assertFalse(HologramLocation.chunkKey(-1, 0) == HologramLocation.chunkKey(0, -1));
        }

        @Test
        void nonFiniteCoordinatesAreRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new HologramLocation("world", Double.NaN, 0, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> new HologramLocation("world", 0, Double.POSITIVE_INFINITY, 0));
            assertThrows(IllegalArgumentException.class, () -> new HologramLocation(" ", 0, 0, 0));
        }
    }

    @Nested
    class Rotations {

        @Test
        void anglesAreWrappedIntoASingleTurn() {
            assertEquals(new EulerRotation(90, 0, 0), new EulerRotation(450, 0, 0).normalized());
            assertEquals(new EulerRotation(270, 0, 0), new EulerRotation(-90, 0, 0).normalized());
        }

        @Test
        void fullTurnsCountAsIdentity() {
            assertTrue(EulerRotation.NONE.isIdentity());
            assertTrue(new EulerRotation(360, -720, 0).isIdentity());
            assertFalse(new EulerRotation(1, 0, 0).isIdentity());
        }
    }
}
