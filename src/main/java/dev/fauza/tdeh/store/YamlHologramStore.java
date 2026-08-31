package dev.fauza.tdeh.store;

import dev.fauza.tdeh.codec.CodecException;
import dev.fauza.tdeh.codec.HologramCodec;
import dev.fauza.tdeh.model.Hologram;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores holograms in {@code data.yml} under a single {@code holograms} section keyed by name.
 *
 * <p>A malformed entry is skipped with a warning rather than aborting the load, so one bad
 * hand-edited hologram does not cost an operator every other hologram on the server.
 */
public final class YamlHologramStore implements HologramStore {

    private static final String ROOT = "holograms";

    private final File file;
    private final Logger logger;

    public YamlHologramStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public Map<String, Hologram> load() {
        Map<String, Hologram> loaded = new LinkedHashMap<>();
        if (!file.exists()) {
            return loaded;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection(ROOT);
        if (root == null) {
            return loaded;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) {
                logger.warning("Skipping hologram '" + name + "': entry is not a mapping");
                continue;
            }
            try {
                Hologram hologram = HologramCodec.decode(name, toMap(section));
                loaded.put(hologram.name(), hologram);
            } catch (CodecException | IllegalArgumentException cause) {
                logger.warning("Skipping hologram '" + name + "': " + cause.getMessage());
            }
        }
        return loaded;
    }

    /**
     * Writes through a temporary file so an interrupted save cannot truncate an existing
     * {@code data.yml} and lose every hologram at once.
     */
    @Override
    public void save(Map<String, Map<String, Object>> snapshot) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection(ROOT);
        snapshot.forEach(root::set);

        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            if (file.getParentFile() != null) {
                Files.createDirectories(file.getParentFile().toPath());
            }
            config.save(temporary);
            move(temporary.toPath(), file.toPath());
        } catch (IOException cause) {
            logger.log(Level.SEVERE, "Failed to write " + file.getName() + "; holograms were not saved", cause);
            // Leaving the temp file behind would shadow the next save attempt's rename.
            if (!temporary.delete() && temporary.exists()) {
                logger.warning("Could not remove stale " + temporary.getName());
            }
        }
    }

    private static void move(java.nio.file.Path source, java.nio.file.Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            // Some network and container filesystems refuse atomic moves; a plain replace still beats
            // writing over the live file in place.
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Flattens a configuration section into plain maps and lists.
     *
     * <p>{@code getValues(true)} is not usable here: it returns nested keys in dotted form, which the
     * codec would read as unknown top-level keys.
     */
    private static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            out.put(key, unwrap(section.get(key)));
        }
        return out;
    }

    private static Object unwrap(Object value) {
        if (value instanceof ConfigurationSection nested) {
            return toMap(nested);
        }
        if (value instanceof List<?> list) {
            List<Object> out = new LinkedList<>();
            for (Object element : list) {
                out.add(unwrap(element));
            }
            return out;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, nested) -> out.put(String.valueOf(key), unwrap(nested)));
            return out;
        }
        return value;
    }
}
