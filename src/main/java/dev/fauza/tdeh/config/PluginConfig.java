package dev.fauza.tdeh.config;

import dev.fauza.tdeh.model.BillboardMode;
import dev.fauza.tdeh.model.TextAlign;
import dev.fauza.tdeh.model.TextPart;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Snapshot of {@code config.yml}. Rebuilt wholesale by {@code /3deh reload} rather than mutated,
 * so a command that started before a reload keeps reading a consistent set of values.
 */
public final class PluginConfig {

    private final BillboardMode defaultBillboard;
    private final float defaultViewRange;
    private final float defaultShadowRadius;
    private final float defaultShadowStrength;
    private final int defaultLineWidth;
    private final boolean defaultSeeThrough;
    private final boolean defaultTextShadow;
    private final TextAlign defaultAlignment;
    private final double selectionMaxDistance;
    private final double selectionMaxAngle;
    private final long autosaveIntervalSeconds;
    private final Messages messages;

    private PluginConfig(Builder builder) {
        this.defaultBillboard = builder.defaultBillboard;
        this.defaultViewRange = builder.defaultViewRange;
        this.defaultShadowRadius = builder.defaultShadowRadius;
        this.defaultShadowStrength = builder.defaultShadowStrength;
        this.defaultLineWidth = builder.defaultLineWidth;
        this.defaultSeeThrough = builder.defaultSeeThrough;
        this.defaultTextShadow = builder.defaultTextShadow;
        this.defaultAlignment = builder.defaultAlignment;
        this.selectionMaxDistance = builder.selectionMaxDistance;
        this.selectionMaxAngle = builder.selectionMaxAngle;
        this.autosaveIntervalSeconds = builder.autosaveIntervalSeconds;
        this.messages = builder.messages;
    }

    /**
     * Reads the plugin's configuration. Unreadable individual values fall back to their built-in
     * default with a warning, because a typo in one colour name should not stop the server from
     * loading its holograms.
     */
    public static PluginConfig load(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Builder builder = new Builder();

        String billboard = config.getString("defaults.billboard", BillboardMode.CENTER.name());
        builder.defaultBillboard = BillboardMode.fromId(billboard).orElseGet(() -> {
            plugin.getLogger().warning("Unknown defaults.billboard '" + billboard + "', using CENTER");
            return BillboardMode.CENTER;
        });
        builder.defaultViewRange = (float) config.getDouble("defaults.view-range", 1.0);
        builder.defaultShadowRadius = (float) config.getDouble("defaults.shadow-radius", 0.0);
        builder.defaultShadowStrength = (float) config.getDouble("defaults.shadow-strength", 1.0);
        builder.defaultLineWidth = Math.max(1, config.getInt("defaults.text.line-width", 200));
        builder.defaultSeeThrough = config.getBoolean("defaults.text.see-through", false);
        builder.defaultTextShadow = config.getBoolean("defaults.text.text-shadow", false);

        String alignment = config.getString("defaults.text.alignment", TextAlign.CENTER.name());
        builder.defaultAlignment = TextAlign.fromId(alignment).orElseGet(() -> {
            plugin.getLogger().warning("Unknown defaults.text.alignment '" + alignment + "', using CENTER");
            return TextAlign.CENTER;
        });

        builder.selectionMaxDistance = Math.max(1.0, config.getDouble("selection.max-distance", 24.0));
        // A cone wider than a right angle would make selection effectively random.
        builder.selectionMaxAngle = clamp(config.getDouble("selection.max-angle", 12.0), 1.0, 89.0);
        builder.autosaveIntervalSeconds = Math.max(0, config.getLong("storage.autosave-interval", 300));

        builder.messages = loadMessages(plugin, config);
        return new PluginConfig(builder);
    }

    private static Messages loadMessages(Plugin plugin, FileConfiguration config) {
        Map<String, String> values = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("messages");
        if (section == null) {
            plugin.getLogger().log(Level.WARNING, "config.yml has no messages section; using raw keys");
            return new Messages(values, "");
        }
        for (String key : section.getKeys(false)) {
            values.put(key, section.getString(key, key));
        }
        String prefix = values.getOrDefault("prefix", "");
        values.remove("prefix");
        return new Messages(values, prefix);
    }

    public Messages messages() {
        return messages;
    }

    public BillboardMode defaultBillboard() {
        return defaultBillboard;
    }

    public double selectionMaxDistance() {
        return selectionMaxDistance;
    }

    public double selectionMaxAngle() {
        return selectionMaxAngle;
    }

    public long autosaveIntervalSeconds() {
        return autosaveIntervalSeconds;
    }

    /** Stamps a freshly created text part with the configured defaults. */
    public void applyDefaults(TextPart part) {
        applyCommonDefaults(part);
        part.lineWidth(defaultLineWidth);
        part.seeThrough(defaultSeeThrough);
        part.textShadow(defaultTextShadow);
        part.alignment(defaultAlignment);
    }

    /** Stamps a freshly created block or item part with the configured defaults. */
    public void applyCommonDefaults(dev.fauza.tdeh.model.HologramPart part) {
        part.billboard(defaultBillboard);
        part.viewRange(Math.max(0f, defaultViewRange));
        part.shadowRadius(Math.max(0f, defaultShadowRadius));
        part.shadowStrength(Math.max(0f, defaultShadowStrength));
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    private static final class Builder {
        private BillboardMode defaultBillboard = BillboardMode.CENTER;
        private float defaultViewRange = 1.0f;
        private float defaultShadowRadius;
        private float defaultShadowStrength = 1.0f;
        private int defaultLineWidth = 200;
        private boolean defaultSeeThrough;
        private boolean defaultTextShadow;
        private TextAlign defaultAlignment = TextAlign.CENTER;
        private double selectionMaxDistance = 24.0;
        private double selectionMaxAngle = 12.0;
        private long autosaveIntervalSeconds = 300;
        private Messages messages = new Messages(Map.of(), "");
    }
}
