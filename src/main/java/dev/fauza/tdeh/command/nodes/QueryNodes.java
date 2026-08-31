package dev.fauza.tdeh.command.nodes;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.fauza.tdeh.command.CommandSupport;
import dev.fauza.tdeh.model.BlockPart;
import dev.fauza.tdeh.model.Hologram;
import dev.fauza.tdeh.model.HologramPart;
import dev.fauza.tdeh.model.ItemPart;
import dev.fauza.tdeh.model.TextPart;
import dev.fauza.tdeh.model.Vec3;
import dev.fauza.tdeh.text.ColorCodec;
import dev.fauza.tdeh.text.TextFormat;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Read-only commands: listing, inspecting, selecting and help.
 */
public final class QueryNodes {

    private static final int PAGE_SIZE = 8;

    /** A help entry and the permission that reveals it. */
    private record HelpLine(String permission, String usage, String description) {
    }

    private static final List<HelpLine> HELP = List.of(
            new HelpLine("3deh.command.create", "/3deh create <text|block|item> <name> <value>",
                    "Create a hologram where you stand"),
            new HelpLine("3deh.command.remove", "/3deh remove [name]", "Delete a hologram"),
            new HelpLine("3deh.command.list", "/3deh list [page]", "List every hologram"),
            new HelpLine("3deh.command.info", "/3deh info [name]", "Show a hologram's parts and attributes"),
            new HelpLine("3deh.command.select", "/3deh select", "Select the hologram you are looking at"),
            new HelpLine("3deh.command.select", "/3deh deselect", "Clear your selection"),
            new HelpLine("3deh.command.move", "/3deh move [name]", "Move a hologram to your position"),
            new HelpLine("3deh.command.teleport", "/3deh teleport <name>", "Teleport yourself to a hologram"),
            new HelpLine("3deh.command.edit", "/3deh edit [name] <attribute> <value>",
                    "Edit the first part of a hologram"),
            new HelpLine("3deh.command.part", "/3deh part add <name> <text|block|item> <value>",
                    "Add a part to a hologram"),
            new HelpLine("3deh.command.part", "/3deh part remove <name> <index>", "Remove a part"),
            new HelpLine("3deh.command.part", "/3deh part list <name>", "List a hologram's parts"),
            new HelpLine("3deh.command.part", "/3deh part offset <name> <index> <x> <y> <z>",
                    "Position a part relative to the anchor"),
            new HelpLine("3deh.command.part", "/3deh part edit <name> <index> <attribute> <value>",
                    "Edit one part of a hologram"),
            new HelpLine("3deh.command.reload", "/3deh reload", "Reload config.yml and data.yml"));

    private QueryNodes() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> help(CommandSupport support) {
        return Commands.literal("help")
                .requires(source -> source.getSender().hasPermission("3deh.command.help"))
                .executes(helpExecutor(support));
    }

    /**
     * The help body, exposed so bare {@code /3deh} can run it without building a second node.
     *
     * <p>Only lines the sender has permission for are printed, so the help always matches what they
     * can actually run.
     */
    public static Command<CommandSourceStack> helpExecutor(CommandSupport support) {
        return context -> {
            CommandSender sender = CommandSupport.senderOf(context);
            sender.sendMessage(Component.text("3DEH (Lite) commands", NamedTextColor.AQUA));
            boolean any = false;
            for (HelpLine line : HELP) {
                if (!sender.hasPermission(line.permission())) {
                    continue;
                }
                any = true;
                sender.sendMessage(Component.text(line.usage(), NamedTextColor.WHITE)
                        .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(line.description(), NamedTextColor.GRAY)));
            }
            if (!any) {
                support.messages().send(sender, "no-permission");
            }
            return Command.SINGLE_SUCCESS;
        };
    }

    public static LiteralArgumentBuilder<CommandSourceStack> list(CommandSupport support) {
        return Commands.literal("list")
                .requires(source -> source.getSender().hasPermission("3deh.command.list"))
                .executes(context -> doList(context, support, 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> doList(context, support,
                                IntegerArgumentType.getInteger(context, "page"))));
    }

    private static int doList(CommandContext<CommandSourceStack> context, CommandSupport support, int page) {
        CommandSender sender = CommandSupport.senderOf(context);
        List<Hologram> all = new ArrayList<>(support.manager().all());
        if (all.isEmpty()) {
            support.messages().send(sender, "list-empty");
            return Command.SINGLE_SUCCESS;
        }
        all.sort((left, right) -> left.name().compareTo(right.name()));

        int pages = (all.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int clamped = Math.min(page, pages);
        sender.sendMessage(support.messages().renderBare("list-header",
                "page", String.valueOf(clamped), "pages", String.valueOf(pages)));

        int from = (clamped - 1) * PAGE_SIZE;
        for (Hologram hologram : all.subList(from, Math.min(from + PAGE_SIZE, all.size()))) {
            sender.sendMessage(Component.text(hologram.name(), NamedTextColor.WHITE)
                    .append(Component.text(" - " + hologram.partCount() + " part(s) - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(describeLocation(hologram), NamedTextColor.GRAY))
                    .append(support.manager().isSpawned(hologram.name())
                            ? Component.empty()
                            : Component.text(" (unloaded)", NamedTextColor.DARK_GRAY)));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> info(CommandSupport support) {
        return Commands.literal("info")
                .requires(source -> source.getSender().hasPermission("3deh.command.info"))
                .executes(context -> doInfo(context, support, support.requireSelected(context)))
                .then(LifecycleNodes.named(support)
                        .executes(context -> doInfo(context, support, support.requireNamed(context))));
    }

    private static int doInfo(CommandContext<CommandSourceStack> context,
                              CommandSupport support,
                              Hologram hologram) {
        if (hologram == null) {
            return 0;
        }
        CommandSender sender = CommandSupport.senderOf(context);
        sender.sendMessage(Component.text(hologram.name(), NamedTextColor.AQUA)
                .append(Component.text(" at " + describeLocation(hologram), NamedTextColor.GRAY)));

        for (int i = 0; i < hologram.partCount(); i++) {
            HologramPart part = hologram.part(i);
            sender.sendMessage(Component.text("#" + i + " " + part.type().id(), NamedTextColor.WHITE)
                    .append(Component.text(" " + describePart(part), NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("   " + describeCommon(part), NamedTextColor.DARK_GRAY));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> select(CommandSupport support) {
        return Commands.literal("select")
                .requires(source -> source.getSender().hasPermission("3deh.command.select"))
                .executes(context -> {
                    Player player = support.requirePlayer(context);
                    if (player == null) {
                        return 0;
                    }
                    var found = support.selections().lookingAt(player,
                            support.plugin().config().selectionMaxDistance(),
                            support.plugin().config().selectionMaxAngle());
                    if (found.isEmpty()) {
                        support.messages().send(player, "nothing-looked-at");
                        return 0;
                    }
                    support.selections().select(player, found.get());
                    support.messages().send(player, "selected", "name", found.get().name());
                    return Command.SINGLE_SUCCESS;
                });
    }

    public static LiteralArgumentBuilder<CommandSourceStack> deselect(CommandSupport support) {
        return Commands.literal("deselect")
                .requires(source -> source.getSender().hasPermission("3deh.command.select"))
                .executes(context -> {
                    Player player = support.requirePlayer(context);
                    if (player == null) {
                        return 0;
                    }
                    support.selections().clear(player);
                    support.messages().send(player, "deselected");
                    return Command.SINGLE_SUCCESS;
                });
    }

    static String describeLocation(Hologram hologram) {
        return String.format(Locale.ROOT, "%s %.1f %.1f %.1f",
                hologram.location().world(),
                hologram.location().x(),
                hologram.location().y(),
                hologram.location().z());
    }

    /** One-line summary of the type-specific attributes, for {@code info} and {@code part list}. */
    static String describePart(HologramPart part) {
        if (part instanceof TextPart text) {
            String preview = TextFormat.plain(text.text()).replace("\n", " / ");
            return "\"" + (preview.length() > 40 ? preview.substring(0, 40) + "..." : preview) + "\"";
        }
        if (part instanceof BlockPart block) {
            return block.blockData();
        }
        if (part instanceof ItemPart item) {
            return item.material() + " (" + item.transform().id() + ")";
        }
        return "";
    }

    private static String describeCommon(HologramPart part) {
        StringBuilder out = new StringBuilder();
        out.append("offset ").append(format(part.offset()));
        out.append(" scale ").append(format(part.scale()));
        out.append(" billboard ").append(part.billboard().id());
        if (!part.rotation().isIdentity()) {
            out.append(" rotation ").append(String.format(Locale.ROOT, "%.0f/%.0f/%.0f",
                    part.rotation().yaw(), part.rotation().pitch(), part.rotation().roll()));
        }
        if (part.glowColor() != null) {
            out.append(" glow ").append(ColorCodec.format(part.glowColor()));
        }
        if (part.brightness() != null) {
            out.append(" brightness ")
                    .append(part.brightness().block()).append('/').append(part.brightness().sky());
        }
        return out.toString();
    }

    private static String format(Vec3 vector) {
        return String.format(Locale.ROOT, "%.2f/%.2f/%.2f", vector.x(), vector.y(), vector.z());
    }
}
