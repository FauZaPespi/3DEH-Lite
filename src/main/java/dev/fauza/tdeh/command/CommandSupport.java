package dev.fauza.tdeh.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.fauza.tdeh.ThreeDehPlugin;
import dev.fauza.tdeh.config.Messages;
import dev.fauza.tdeh.manager.HologramManager;
import dev.fauza.tdeh.manager.SelectionService;
import dev.fauza.tdeh.model.Hologram;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Shared plumbing for the command tree: resolving which hologram a command is about, and turning a
 * failure into a message plus a zero result rather than a Brigadier exception.
 *
 * <p>Reads {@code plugin.config()} on every call instead of caching it, because {@code /3deh reload}
 * swaps the whole configuration object.
 */
public final class CommandSupport {

    /** The hologram and part a command is acting on. */
    public record Target(Hologram hologram, int partIndex) {
    }

    /** Resolves the target of an attribute command, or reports why it could not. */
    @FunctionalInterface
    public interface PartTarget {
        Target resolve(CommandContext<CommandSourceStack> context);
    }

    public static final String NAME_ARGUMENT = "hologram";

    private final ThreeDehPlugin plugin;

    public CommandSupport(ThreeDehPlugin plugin) {
        this.plugin = plugin;
    }

    public ThreeDehPlugin plugin() {
        return plugin;
    }

    public HologramManager manager() {
        return plugin.manager();
    }

    public SelectionService selections() {
        return plugin.selections();
    }

    public Messages messages() {
        return plugin.config().messages();
    }

    public static CommandSender senderOf(CommandContext<CommandSourceStack> context) {
        return context.getSource().getSender();
    }

    /** @return the sender as a player, or null after telling them the command needs one. */
    public Player requirePlayer(CommandContext<CommandSourceStack> context) {
        CommandSender sender = senderOf(context);
        if (sender instanceof Player player) {
            return player;
        }
        messages().send(sender, "players-only");
        return null;
    }

    /** @return the named hologram, or null after reporting that it does not exist. */
    public Hologram requireNamed(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, NAME_ARGUMENT);
        Optional<Hologram> hologram = manager().find(name);
        if (hologram.isEmpty()) {
            messages().send(senderOf(context), "unknown-hologram", "name", name);
            return null;
        }
        return hologram.get();
    }

    /**
     * @return the player's current selection, or null after telling them to select something. Used by
     *         the command forms that omit the hologram name.
     */
    public Hologram requireSelected(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return null;
        }
        Optional<Hologram> selected = selections().selected(player);
        if (selected.isEmpty()) {
            messages().send(player, "no-selection");
            return null;
        }
        return selected.get();
    }

    /** Targets part 0 of the player's selection: the {@code /3deh edit <attribute>} form. */
    public Target selectedFirstPart(CommandContext<CommandSourceStack> context) {
        Hologram hologram = requireSelected(context);
        return hologram == null ? null : new Target(hologram, 0);
    }

    /** Targets part 0 of a named hologram: the {@code /3deh edit <name> <attribute>} form. */
    public Target namedFirstPart(CommandContext<CommandSourceStack> context) {
        Hologram hologram = requireNamed(context);
        return hologram == null ? null : new Target(hologram, 0);
    }

    /** Validates a part index against a hologram, reporting the valid range when it is out of bounds. */
    public Target namedPart(CommandContext<CommandSourceStack> context, int partIndex) {
        Hologram hologram = requireNamed(context);
        if (hologram == null) {
            return null;
        }
        if (partIndex < 0 || partIndex >= hologram.partCount()) {
            messages().send(senderOf(context), "part-not-found",
                    "name", hologram.name(), "index", String.valueOf(partIndex));
            return null;
        }
        return new Target(hologram, partIndex);
    }
}
