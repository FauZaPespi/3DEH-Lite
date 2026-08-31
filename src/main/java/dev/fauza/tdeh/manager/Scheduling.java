package dev.fauza.tdeh.manager;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The plugin's only entry point to scheduling.
 *
 * <p>Paper exposes the Folia region schedulers on ordinary Paper too and translates them onto its
 * own main thread internally, so there is no Paper-versus-Folia branch anywhere in this plugin:
 * every task goes through the regional API and is correct on both. The corollary is that
 * {@code BukkitScheduler} must never be used, since it has no meaning on Folia.
 *
 * <p>Which scheduler to pick is not interchangeable:
 * <ul>
 *   <li>work anchored to a position uses {@link #atLocation};</li>
 *   <li>work touching an existing entity uses {@link #forEntity}, because an entity can cross into
 *       another region between the moment the task is queued and the moment it runs;</li>
 *   <li>disk I/O uses {@link #async}, and must never touch a Bukkit object.</li>
 * </ul>
 */
public final class Scheduling {

    private final Plugin plugin;

    public Scheduling(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Runs on the thread owning the region that contains {@code location}. */
    public void atLocation(Location location, Runnable task) {
        plugin.getServer().getRegionScheduler().execute(plugin, location, task);
    }

    /**
     * Runs on the thread currently owning {@code entity}.
     *
     * <p>The retired callback fires instead when the entity is gone by the time the task would run,
     * which is routine here: a chunk can unload between queuing and execution.
     */
    public void forEntity(Entity entity, Runnable task, Runnable retired) {
        entity.getScheduler().run(plugin, ignored -> task.run(), retired);
    }

    /** Runs off the tick loop. Nothing Bukkit-owned may be touched from here. */
    public void async(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, ignored -> task.run());
    }

    /** Repeats off the tick loop, for the autosave timer. */
    public ScheduledTask asyncRepeating(Consumer<ScheduledTask> task, long intervalSeconds) {
        return plugin.getServer().getAsyncScheduler()
                .runAtFixedRate(plugin, task, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    /** Runs on the global region, for work that belongs to no particular position. */
    public void global(Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
    }
}
