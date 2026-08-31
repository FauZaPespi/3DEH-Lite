package dev.fauza.tdeh.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Name rules for holograms. Names are the primary key in {@code data.yml} and a YAML section key,
 * so the allowed charset is deliberately narrow, and lookups are case-insensitive to stop
 * {@code Shop} and {@code shop} from becoming two holograms an admin cannot tell apart.
 */
public final class HologramName {

    public static final int MAX_LENGTH = 32;

    private static final Pattern VALID = Pattern.compile("[a-zA-Z0-9_-]{1," + MAX_LENGTH + "}");

    private HologramName() {
    }

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    /** @return the lower-cased form used as the registry key. */
    public static String canonical(String name) {
        if (!isValid(name)) {
            throw new IllegalArgumentException("Invalid hologram name: " + name);
        }
        return name.toLowerCase(Locale.ROOT);
    }
}
