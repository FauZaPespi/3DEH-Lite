package dev.fauza.tdeh.codec;

import dev.fauza.tdeh.model.BillboardMode;
import dev.fauza.tdeh.model.BlockPart;
import dev.fauza.tdeh.model.Brightness;
import dev.fauza.tdeh.model.DisplayType;
import dev.fauza.tdeh.model.EulerRotation;
import dev.fauza.tdeh.model.Hologram;
import dev.fauza.tdeh.model.HologramLocation;
import dev.fauza.tdeh.model.HologramPart;
import dev.fauza.tdeh.model.ItemPart;
import dev.fauza.tdeh.model.ItemTransform;
import dev.fauza.tdeh.model.TextAlign;
import dev.fauza.tdeh.model.TextPart;
import dev.fauza.tdeh.model.Vec3;
import dev.fauza.tdeh.text.ColorCodec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a {@link Hologram} to and from the plain maps that go into {@code data.yml}.
 *
 * <p>Encoding omits every attribute still at its default value. That keeps hand-editing practical:
 * a freshly created text hologram is six lines rather than twenty-five, and the file only grows
 * with the attributes an operator actually changed.
 *
 * <p>No Bukkit types appear here, so the whole round-trip is covered by plain unit tests.
 */
public final class HologramCodec {

    private static final String KEY_WORLD = "world";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";
    private static final String KEY_PARTS = "parts";
    private static final String KEY_TYPE = "type";

    private HologramCodec() {
    }

    public static Map<String, Object> encode(Hologram hologram) {
        Map<String, Object> out = new LinkedHashMap<>();
        HologramLocation location = hologram.location();
        out.put(KEY_WORLD, location.world());
        out.put(KEY_X, location.x());
        out.put(KEY_Y, location.y());
        out.put(KEY_Z, location.z());

        List<Map<String, Object>> parts = new ArrayList<>(hologram.partCount());
        for (HologramPart part : hologram.parts()) {
            parts.add(encodePart(part));
        }
        out.put(KEY_PARTS, parts);
        return out;
    }

    public static Hologram decode(String name, Map<?, ?> data) {
        HologramLocation location = new HologramLocation(
                Values.requireString(data, KEY_WORLD),
                Values.requireDouble(data, KEY_X),
                Values.requireDouble(data, KEY_Y),
                Values.requireDouble(data, KEY_Z));

        Object rawParts = data.get(KEY_PARTS);
        if (!(rawParts instanceof List<?> list) || list.isEmpty()) {
            throw new CodecException("Hologram '" + name + "' has no parts");
        }
        List<HologramPart> parts = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof Map<?, ?> partMap)) {
                throw new CodecException("Hologram '" + name + "' part #" + i + " is not a mapping");
            }
            try {
                parts.add(decodePart(partMap));
            } catch (CodecException | IllegalArgumentException cause) {
                throw new CodecException(
                        "Hologram '" + name + "' part #" + i + ": " + cause.getMessage(), cause);
            }
        }
        return new Hologram(name, location, parts);
    }

    private static Map<String, Object> encodePart(HologramPart part) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(KEY_TYPE, part.type().id());

        putIfChanged(out, "offset", Values.toList(part.offset()), Values.toList(Vec3.ZERO));
        putIfChanged(out, "scale", Values.toList(part.scale()), Values.toList(Vec3.ONE));
        putIfChanged(out, "translation", Values.toList(part.translation()), Values.toList(Vec3.ZERO));
        EulerRotation rotation = part.rotation();
        putIfChanged(out, "rotation",
                List.of(rotation.yaw(), rotation.pitch(), rotation.roll()),
                List.of(0.0, 0.0, 0.0));
        putIfChanged(out, "billboard", part.billboard().name(), BillboardMode.CENTER.name());
        putIfChanged(out, "view-range", part.viewRange(), 1.0f);
        putIfChanged(out, "shadow-radius", part.shadowRadius(), 0.0f);
        putIfChanged(out, "shadow-strength", part.shadowStrength(), 1.0f);
        putIfChanged(out, "width", part.displayWidth(), 0.0f);
        putIfChanged(out, "height", part.displayHeight(), 0.0f);
        putIfChanged(out, "interpolation-delay", part.interpolationDelay(), 0);
        putIfChanged(out, "interpolation-duration", part.interpolationDuration(), 0);
        putIfChanged(out, "teleport-duration", part.teleportDuration(), 0);
        if (part.glowColor() != null) {
            out.put("glow", ColorCodec.format(part.glowColor()));
        }
        if (part.brightness() != null) {
            out.put("brightness", List.of(part.brightness().block(), part.brightness().sky()));
        }

        if (part instanceof TextPart text) {
            encodeText(out, text);
        } else if (part instanceof BlockPart block) {
            out.put("block", block.blockData());
        } else if (part instanceof ItemPart item) {
            out.put("item", item.material());
            putIfChanged(out, "item-transform", item.transform().name(), ItemTransform.FIXED.name());
        } else {
            throw new CodecException("Unsupported part class " + part.getClass().getName());
        }
        return out;
    }

    private static void encodeText(Map<String, Object> out, TextPart text) {
        out.put("text", text.text());
        putIfChanged(out, "line-width", text.lineWidth(), 200);
        putIfChanged(out, "opacity", text.opacity(), TextPart.DEFAULT_OPACITY);
        putIfChanged(out, "text-shadow", text.textShadow(), false);
        putIfChanged(out, "see-through", text.seeThrough(), false);
        putIfChanged(out, "default-background", text.defaultBackground(), true);
        if (text.backgroundColor() != null) {
            out.put("background", ColorCodec.formatArgb(text.backgroundColor()));
        }
        putIfChanged(out, "alignment", text.alignment().name(), TextAlign.CENTER.name());
    }

    private static HologramPart decodePart(Map<?, ?> data) {
        String rawType = Values.requireString(data, KEY_TYPE);
        DisplayType type = DisplayType.fromId(rawType)
                .orElseThrow(() -> new CodecException("Unknown part type '" + rawType + "'"));

        HologramPart part = switch (type) {
            case TEXT -> decodeText(data);
            case BLOCK -> new BlockPart(Values.requireString(data, "block"));
            case ITEM -> decodeItem(data);
        };
        applyCommon(part, data);
        return part;
    }

    private static TextPart decodeText(Map<?, ?> data) {
        TextPart text = new TextPart(Values.requireString(data, "text"));
        text.lineWidth(Values.optInt(data, "line-width", 200));
        text.opacity(Values.optInt(data, "opacity", TextPart.DEFAULT_OPACITY));
        text.textShadow(Values.optBoolean(data, "text-shadow", false));
        text.seeThrough(Values.optBoolean(data, "see-through", false));
        String background = Values.optString(data, "background", null);
        if (background != null) {
            text.backgroundColor(ColorCodec.parseArgb(background)
                    .orElseThrow(() -> new CodecException("Key 'background' is not a colour: " + background)));
        }
        // Read after the background, which flips it off when an explicit colour is set.
        text.defaultBackground(Values.optBoolean(data, "default-background", text.defaultBackground()));
        String alignment = Values.optString(data, "alignment", TextAlign.CENTER.name());
        text.alignment(TextAlign.fromId(alignment)
                .orElseThrow(() -> new CodecException("Unknown alignment '" + alignment + "'")));
        return text;
    }

    private static ItemPart decodeItem(Map<?, ?> data) {
        ItemPart item = new ItemPart(Values.requireString(data, "item"));
        String transform = Values.optString(data, "item-transform", ItemTransform.FIXED.name());
        item.transform(ItemTransform.fromId(transform)
                .orElseThrow(() -> new CodecException("Unknown item transform '" + transform + "'")));
        return item;
    }

    private static void applyCommon(HologramPart part, Map<?, ?> data) {
        part.offset(Values.optVec3(data, "offset", Vec3.ZERO));
        part.scale(Values.optVec3(data, "scale", Vec3.ONE));
        part.translation(Values.optVec3(data, "translation", Vec3.ZERO));

        Vec3 rotation = Values.optVec3(data, "rotation", Vec3.ZERO);
        part.rotation(new EulerRotation(rotation.x(), rotation.y(), rotation.z()));

        String billboard = Values.optString(data, "billboard", BillboardMode.CENTER.name());
        part.billboard(BillboardMode.fromId(billboard)
                .orElseThrow(() -> new CodecException("Unknown billboard mode '" + billboard + "'")));

        part.viewRange(Values.optFloat(data, "view-range", 1.0f));
        part.shadowRadius(Values.optFloat(data, "shadow-radius", 0.0f));
        part.shadowStrength(Values.optFloat(data, "shadow-strength", 1.0f));
        part.displayWidth(Values.optFloat(data, "width", 0.0f));
        part.displayHeight(Values.optFloat(data, "height", 0.0f));
        part.interpolationDelay(Values.optInt(data, "interpolation-delay", 0));
        part.interpolationDuration(Values.optInt(data, "interpolation-duration", 0));
        part.teleportDuration(Values.optInt(data, "teleport-duration", 0));

        String glow = Values.optString(data, "glow", null);
        if (glow != null && !ColorCodec.isNone(glow)) {
            part.glowColor(ColorCodec.parse(glow)
                    .orElseThrow(() -> new CodecException("Key 'glow' is not a colour: " + glow)));
        }

        Object brightness = data.get("brightness");
        if (brightness != null) {
            if (!(brightness instanceof List<?> levels) || levels.size() != 2) {
                throw new CodecException("Key 'brightness' must be a list of two levels [block, sky]");
            }
            part.brightness(new Brightness(intOf(levels.get(0)), intOf(levels.get(1))));
        }
    }

    private static int intOf(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new CodecException("Brightness levels must be numbers, got " + value);
    }

    private static void putIfChanged(Map<String, Object> out, String key, Object value, Object defaultValue) {
        if (!value.equals(defaultValue)) {
            out.put(key, value);
        }
    }
}
