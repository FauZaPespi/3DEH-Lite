package dev.fauza.tdeh.command.nodes;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.fauza.tdeh.command.CommandSupport;
import dev.fauza.tdeh.command.args.Suggestions;
import dev.fauza.tdeh.model.Hologram;
import dev.fauza.tdeh.model.HologramLocation;
import dev.fauza.tdeh.model.HologramName;
import dev.fauza.tdeh.model.HologramPart;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.function.Function;

/**
 * Commands that create, delete or move whole holograms.
 */
public final class LifecycleNodes {

    private LifecycleNodes() {
    }

    /** {@code /3deh create <type> <name> <value...>} anchored at the player's feet. */
    public static LiteralArgumentBuilder<CommandSourceStack> create(CommandSupport support) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("create")
                .requires(source -> source.getSender().hasPermission("3deh.command.create"));

        node.then(createBranch(support, "text", "text", StringArgumentType.greedyString(), null,
                PartFactory::text));
        node.then(createBranch(support, "block", "data", StringArgumentType.greedyString(),
                Suggestions.blockMaterials(), PartFactory::block));
        node.then(createBranch(support, "item", "material", StringArgumentType.word(),
                Suggestions.itemMaterials(), PartFactory::item));
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createBranch(
            CommandSupport support,
            String typeLiteral,
            String valueArgument,
            com.mojang.brigadier.arguments.ArgumentType<String> valueType,
            com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> suggestions,
            Function<String, HologramPart> factory) {

        var value = Commands.<String>argument(valueArgument, valueType)
                .executes(context -> doCreate(context, support, valueArgument, factory));
        if (suggestions != null) {
            value.suggests(suggestions);
        }
        return Commands.literal(typeLiteral)
                .then(Commands.argument(CommandSupport.NAME_ARGUMENT, StringArgumentType.word())
                        .then(value));
    }

    private static int doCreate(CommandContext<CommandSourceStack> context,
                                CommandSupport support,
                                String valueArgument,
                                Function<String, HologramPart> factory) {
        Player player = support.requirePlayer(context);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, CommandSupport.NAME_ARGUMENT);
        if (!HologramName.isValid(name)) {
            support.messages().send(player, "invalid-name", "name", name);
            return 0;
        }
        if (support.manager().exists(name)) {
            support.messages().send(player, "name-taken", "name", name);
            return 0;
        }

        HologramPart part;
        try {
            part = factory.apply(StringArgumentType.getString(context, valueArgument));
        } catch (IllegalArgumentException rejected) {
            player.sendMessage(Component.text(rejected.getMessage(), NamedTextColor.RED));
            return 0;
        }
        support.plugin().config().applyCommonDefaults(part);
        if (part instanceof dev.fauza.tdeh.model.TextPart text) {
            support.plugin().config().applyDefaults(text);
        }

        Location at = player.getLocation();
        Hologram hologram = new Hologram(name,
                new HologramLocation(at.getWorld().getName(), at.getX(), at.getY(), at.getZ()), part);
        support.manager().create(hologram);
        support.manager().requestSave();
        support.selections().select(player, hologram);
        support.messages().send(player, "created", "name", hologram.name());
        return Command.SINGLE_SUCCESS;
    }

    /** {@code /3deh remove [name]}, falling back to the player's selection. */
    public static LiteralArgumentBuilder<CommandSourceStack> remove(CommandSupport support) {
        return Commands.literal("remove")
                .requires(source -> source.getSender().hasPermission("3deh.command.remove"))
                .executes(context -> doRemove(context, support, support.requireSelected(context)))
                .then(named(support)
                        .executes(context -> doRemove(context, support, support.requireNamed(context))));
    }

    private static int doRemove(CommandContext<CommandSourceStack> context,
                                CommandSupport support,
                                Hologram hologram) {
        if (hologram == null) {
            return 0;
        }
        support.manager().remove(hologram);
        support.manager().requestSave();
        support.messages().send(CommandSupport.senderOf(context), "removed", "name", hologram.name());
        return Command.SINGLE_SUCCESS;
    }

    /** {@code /3deh move [name]} re-anchors a hologram at the player's position. */
    public static LiteralArgumentBuilder<CommandSourceStack> move(CommandSupport support) {
        return Commands.literal("move")
                .requires(source -> source.getSender().hasPermission("3deh.command.move"))
                .executes(context -> doMove(context, support, support.requireSelected(context)))
                .then(named(support)
                        .executes(context -> doMove(context, support, support.requireNamed(context))));
    }

    private static int doMove(CommandContext<CommandSourceStack> context,
                              CommandSupport support,
                              Hologram hologram) {
        Player player = support.requirePlayer(context);
        if (player == null || hologram == null) {
            return 0;
        }
        Location at = player.getLocation();
        HologramLocation destination =
                new HologramLocation(at.getWorld().getName(), at.getX(), at.getY(), at.getZ());
        // Moving changes which chunk owns the hologram, so the index and the entities are rebuilt.
        support.manager().restructure(hologram, () -> hologram.location(destination));
        support.manager().requestSave();
        support.messages().send(player, "moved", "name", hologram.name());
        return Command.SINGLE_SUCCESS;
    }

    /** {@code /3deh teleport <name>} moves the player to a hologram. */
    public static LiteralArgumentBuilder<CommandSourceStack> teleport(CommandSupport support) {
        return Commands.literal("teleport")
                .requires(source -> source.getSender().hasPermission("3deh.command.teleport"))
                .then(named(support).executes(context -> {
                    Player player = support.requirePlayer(context);
                    Hologram hologram = support.requireNamed(context);
                    if (player == null || hologram == null) {
                        return 0;
                    }
                    var world = support.plugin().getServer().getWorld(hologram.location().world());
                    if (world == null) {
                        player.sendMessage(Component.text(
                                "World '" + hologram.location().world() + "' is not loaded.", NamedTextColor.RED));
                        return 0;
                    }
                    // teleportAsync is the only form that works across regions on Folia.
                    player.teleportAsync(dev.fauza.tdeh.manager.HologramManager
                            .toLocation(world, hologram.location()));
                    return Command.SINGLE_SUCCESS;
                }));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> reload(CommandSupport support) {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("3deh.command.reload"))
                .executes(context -> {
                    int loaded = support.plugin().reloadEverything();
                    support.messages().send(CommandSupport.senderOf(context),
                            "reloaded", "count", String.valueOf(loaded));
                    return Command.SINGLE_SUCCESS;
                });
    }

    /** The shared {@code <hologram>} argument, complete with name suggestions. */
    public static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> named(
            CommandSupport support) {
        return Commands.<String>argument(CommandSupport.NAME_ARGUMENT, StringArgumentType.word())
                .suggests(Suggestions.hologramNames(support.manager()));
    }

    /** Builds the initial part of a new hologram from the value typed on the command line. */
    private static final class PartFactory {

        private PartFactory() {
        }

        static HologramPart text(String value) {
            return new dev.fauza.tdeh.model.TextPart(value);
        }

        static HologramPart block(String value) {
            return new dev.fauza.tdeh.model.BlockPart(value);
        }

        static HologramPart item(String value) {
            return new dev.fauza.tdeh.model.ItemPart(value);
        }
    }
}
