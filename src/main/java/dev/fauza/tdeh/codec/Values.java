package dev.fauza.tdeh.codec;

import dev.fauza.tdeh.model.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Typed reads over the loosely-typed maps SnakeYAML produces. Every accessor names the key it
 * failed on, because these run against files humans edit by hand.
 */
final class Values {

    private Values() {
    }

    static String requireString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new CodecException("Missing required key '" + key + "'");
        }
        return String.valueOf(value);
    }

    static String optString(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    static double requireDouble(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new CodecException("Missing required key '" + key + "'");
        }
        return toDouble(value, key);
    }

    static double optDouble(Map<?, ?> map, String key, double fallback) {
        Object value = map.get(key);
        return value == null ? fallback : toDouble(value, key);
    }

    static float optFloat(Map<?, ?> map, String key, float fallback) {
        return (float) optDouble(map, key, fallback);
    }

    static int optInt(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        double raw = toDouble(value, key);
        if (raw != Math.rint(raw)) {
            throw new CodecException("Key '" + key + "' must be a whole number, got " + value);
        }
        return (int) raw;
    }

    static boolean optBoolean(Map<?, ?> map, String key, boolean fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value);
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.parseBoolean(text);
        }
        throw new CodecException("Key '" + key + "' must be true or false, got " + value);
    }

    /** Reads a three-element numeric list, the form used for offsets, scale and rotation. */
    static Vec3 optVec3(Map<?, ?> map, String key, Vec3 fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof List<?> list) || list.size() != 3) {
            throw new CodecException("Key '" + key + "' must be a list of three numbers, got " + value);
        }
        return new Vec3(toDouble(list.get(0), key), toDouble(list.get(1), key), toDouble(list.get(2), key));
    }

    static List<Double> toList(Vec3 vector) {
        return List.of(vector.x(), vector.y(), vector.z());
    }

    private static double toDouble(Object value, String key) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException cause) {
            throw new CodecException("Key '" + key + "' must be a number, got " + value, cause);
        }
    }
}
