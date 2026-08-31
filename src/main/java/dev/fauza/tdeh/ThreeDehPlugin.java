package dev.fauza.tdeh;

import dev.fauza.tdeh.command.ThreeDehCommand;
import dev.fauza.tdeh.config.PluginConfig;
import dev.fauza.tdeh.listener.ChunkLifecycleListener;
import dev.fauza.tdeh.manager.HologramManager;
import dev.fauza.tdeh.manager.Scheduling;
import dev.fauza.tdeh.manager.SelectionService;
import dev.fauza.tdeh.render.DisplayRenderer;
import dev.fauza.tdeh.render.EntityTagger;
import dev.fauza.tdeh.store.YamlHologramStore;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Entry point for 3DEH (Lite).
 *
 * <p>Requires Paper 1.20.6 or newer and runs unmodified on Folia: all scheduling goes through the
 * regional API in {@link Scheduling}, which Paper implements on top of its own main thread.
 */
public final class ThreeDehPlugin extends JavaPlugin {

    private PluginConfig config;
    private Scheduling scheduling;
    private HologramManager manager;
    private SelectionService selections;
    private ScheduledTask autosaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = PluginConfig.load(this);

        scheduling = new Scheduling(this);
        EntityTagger tagger = new EntityTagger(this);
        manager = new HologramManager(
                getServer(),
                getLogger(),
                scheduling,
                new DisplayRenderer(tagger),
                tagger,
                new YamlHologramStore(new File(getDataFolder(), "data.yml"), getLogger()));
        selections = new SelectionService(manager);

        getServer().getPluginManager().registerEvents(
                new ChunkLifecycleListener(manager, selections), this);
        new ThreeDehCommand(this).register();

        int loaded = manager.reload();
        getLogger().info("Loaded " + loaded + " hologram(s).");
        startAutosave();
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        if (manager != null) {
            manager.flush();
            // Every scheduler refuses new tasks once the plugin is disabled, so the scheduler-based
            // despawnAll() would throw here; see despawnAllImmediate() for why direct removal is safe.
            manager.despawnAllImmediate();
        }
    }

    private void startAutosave() {
        long interval = config.autosaveIntervalSeconds();
        if (interval <= 0) {
            return;
        }
        autosaveTask = scheduling.asyncRepeating(task -> manager.flush(), interval);
    }

    /** Re-reads {@code config.yml} and {@code data.yml}. @return the number of holograms loaded. */
    public int reloadEverything() {
        reloadConfig();
        config = PluginConfig.load(this);
        int loaded = manager.reload();

        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        startAutosave();
        return loaded;
    }

    public PluginConfig config() {
        return config;
    }

    public HologramManager manager() {
        return manager;
    }

    public SelectionService selections() {
        return selections;
    }

    public Scheduling scheduling() {
        return scheduling;
    }
}
