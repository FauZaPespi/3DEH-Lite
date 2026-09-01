package dev.fauza.tdeh.render;

import dev.fauza.tdeh.math.BlockPivot;
import dev.fauza.tdeh.math.RotationMath;
import dev.fauza.tdeh.model.BillboardMode;
import dev.fauza.tdeh.model.BlockPart;
import dev.fauza.tdeh.model.Brightness;
import dev.fauza.tdeh.model.HologramPart;
import dev.fauza.tdeh.model.ItemPart;
import dev.fauza.tdeh.model.ItemTransform;
import dev.fauza.tdeh.model.TextAlign;
import dev.fauza.tdeh.model.TextPart;
import dev.fauza.tdeh.model.Vec3;
import dev.fauza.tdeh.text.TextFormat;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

/**
 * Turns a {@link HologramPart} into a live display entity and keeps that entity in sync with it.
 *
 * <p>Every method here must run on the thread owning the region the entity lives in; callers go
 * through {@code Scheduling} to guarantee that.
 *
 * <p>Model enums are mapped to their Bukkit counterparts with exhaustive switches rather than
 * {@code valueOf(name())}. It is more code, but a constant renamed upstream then breaks the build
 * instead of throwing on a server months later.
 */
public final class DisplayRenderer {

    private final EntityTagger tagger;

    public DisplayRenderer(EntityTagger tagger) {
        this.tagger = tagger;
    }

    /**
     * Spawns the entity for a part.
     *
     * <p>Attributes are applied inside the spawn consumer, which runs before the entity is added to
     * the world, so clients never receive a frame showing an unconfigured display.
     */
    public Display spawn(World world, Location location, HologramPart part, String hologramName, int partIndex) {
        Class<? extends Display> type = switch (part.type()) {
            case TEXT -> TextDisplay.class;
            case BLOCK -> BlockDisplay.class;
            case ITEM -> ItemDisplay.class;
        };
        return world.spawn(location, type, display -> {
            // Never written to a region file, so a crash or an abrupt stop cannot orphan it.
            display.setPersistent(false);
            tagger.tag(display, hologramName, partIndex);
            apply(display, part);
        });
    }

    /** Pushes the current state of a part onto an already spawned entity. */
    public void apply(Display display, HologramPart part) {
        // Block models hang off their entity position by a corner; the others are already centred.
        Vector3f translation = part instanceof BlockPart
                ? BlockPivot.centeringTranslation(part.translation(), part.scale(), part.rotation())
                : toVector(part.translation());

        display.setTransformation(new Transformation(
                translation,
                RotationMath.toQuaternion(part.rotation()),
                toVector(part.scale()),
                RotationMath.toQuaternion(dev.fauza.tdeh.model.EulerRotation.NONE)));

        display.setBillboard(toBukkit(part.billboard()));
        display.setViewRange(part.viewRange());
        display.setShadowRadius(part.shadowRadius());
        display.setShadowStrength(part.shadowStrength());
        display.setDisplayWidth(part.displayWidth());
        display.setDisplayHeight(part.displayHeight());
        display.setInterpolationDelay(part.interpolationDelay());
        display.setInterpolationDuration(part.interpolationDuration());
        display.setTeleportDuration(part.teleportDuration());

        Integer glow = part.glowColor();
        // A glow colour override is invisible unless the entity is actually glowing.
        display.setGlowing(glow != null);
        display.setGlowColorOverride(glow == null ? null : Color.fromRGB(glow));

        Brightness brightness = part.brightness();
        display.setBrightness(brightness == null
                ? null
                : new Display.Brightness(brightness.block(), brightness.sky()));

        if (display instanceof TextDisplay text && part instanceof TextPart textPart) {
            applyText(text, textPart);
        } else if (display instanceof BlockDisplay block && part instanceof BlockPart blockPart) {
            block.setBlock(parseBlockData(blockPart.blockData()));
        } else if (display instanceof ItemDisplay item && part instanceof ItemPart itemPart) {
            applyItem(item, itemPart);
        }
    }

    /**
     * {@code setBackgroundColor} is deprecated upstream and flagged as subject to change, but it is
     * the only way to colour a text background from the API, and 3DEH keeps working if it changes
     * because the value is only ever written, never read back.
     */
    @SuppressWarnings("deprecation")
    private void applyText(TextDisplay display, TextPart part) {
        display.text(TextFormat.render(part.text()));
        display.setLineWidth(part.lineWidth());
        display.setTextOpacity((byte) part.opacity());
        display.setShadowed(part.textShadow());
        display.setSeeThrough(part.seeThrough());
        display.setDefaultBackground(part.defaultBackground());
        display.setAlignment(toBukkit(part.alignment()));

        Integer background = part.backgroundColor();
        if (background != null) {
            display.setBackgroundColor(Color.fromARGB(
                    (background >> 24) & 0xFF,
                    (background >> 16) & 0xFF,
                    (background >> 8) & 0xFF,
                    background & 0xFF));
        }
    }

    private void applyItem(ItemDisplay display, ItemPart part) {
        Material material = Material.matchMaterial(part.material());
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("Unknown item material '" + part.material() + "'");
        }
        display.setItemStack(new ItemStack(material));
        display.setItemDisplayTransform(toBukkit(part.transform()));
    }

    private static BlockData parseBlockData(String raw) {
        try {
            return org.bukkit.Bukkit.createBlockData(raw);
        } catch (IllegalArgumentException cause) {
            throw new IllegalArgumentException("Invalid block data '" + raw + "': " + cause.getMessage(), cause);
        }
    }

    private static Vector3f toVector(Vec3 vector) {
        return new Vector3f((float) vector.x(), (float) vector.y(), (float) vector.z());
    }

    private static Display.Billboard toBukkit(BillboardMode mode) {
        return switch (mode) {
            case FIXED -> Display.Billboard.FIXED;
            case VERTICAL -> Display.Billboard.VERTICAL;
            case HORIZONTAL -> Display.Billboard.HORIZONTAL;
            case CENTER -> Display.Billboard.CENTER;
        };
    }

    private static TextDisplay.TextAlignment toBukkit(TextAlign align) {
        return switch (align) {
            case CENTER -> TextDisplay.TextAlignment.CENTER;
            case LEFT -> TextDisplay.TextAlignment.LEFT;
            case RIGHT -> TextDisplay.TextAlignment.RIGHT;
        };
    }

    private static ItemDisplay.ItemDisplayTransform toBukkit(ItemTransform transform) {
        return switch (transform) {
            case NONE -> ItemDisplay.ItemDisplayTransform.NONE;
            case THIRDPERSON_LEFTHAND -> ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND;
            case THIRDPERSON_RIGHTHAND -> ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND;
            case FIRSTPERSON_LEFTHAND -> ItemDisplay.ItemDisplayTransform.FIRSTPERSON_LEFTHAND;
            case FIRSTPERSON_RIGHTHAND -> ItemDisplay.ItemDisplayTransform.FIRSTPERSON_RIGHTHAND;
            case HEAD -> ItemDisplay.ItemDisplayTransform.HEAD;
            case GUI -> ItemDisplay.ItemDisplayTransform.GUI;
            case GROUND -> ItemDisplay.ItemDisplayTransform.GROUND;
            case FIXED -> ItemDisplay.ItemDisplayTransform.FIXED;
        };
    }
}
