package dev.fauza.tdeh.command.nodes;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.fauza.tdeh.command.CommandSupport;
import dev.fauza.tdeh.command.CommandSupport.Target;
import dev.fauza.tdeh.command.args.Suggestions;
import dev.fauza.tdeh.model.BlockPart;
import dev.fauza.tdeh.model.Hologram;
import dev.fauza.tdeh.model.HologramPart;
import dev.fauza.tdeh.model.ItemPart;
import dev.fauza.tdeh.model.TextPart;
import dev.fauza.tdeh.model.Vec3;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.function.Function;

/**
 * {@code /3deh part ...}: managing the individual display entities inside a hologram.
 *
 * <p>Adding or removing a part renumbers the ones after it, and those numbers index the live entity
 * list, so both go through {@code restructure} and respawn the hologram rather than editing in place.
 */
public final class PartNodes {

    private static final String INDEX_ARGUMENT = "index";

    private PartNodes() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> part(CommandSupport support) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("part")
                .requires(source -> source.getSender().hasPermission("3deh.command.part"));

        node.then(add(support));
        node.then(remove(support));
        node.then(list(support));
        node.then(offset(support));
        node.then(edit(support));
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> add(CommandSupport support) {
        LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal("add");
        add.then(addBranch(support, "text", "text", StringArgumentType.greedyString(), null, TextPart::new));
        add.then(addBranch(support, "block", "data", StringArgumentType.greedyString(),
                Suggestions.blockMaterials(), BlockPart::new));
        add.then(addBranch(support, "item", "material", StringArgumentType.word(),
                Suggestions.itemMaterials(), ItemPart::new));
        return add;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> addBranch(
            CommandSupport support,
            String typeLiteral,
            String valueArgument,
            com.mojang.brigadier.arguments.ArgumentType<String> valueType,
            com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> suggestions,
            Function<String, HologramPart> factory) {

        var value = Commands.<String>argument(valueArgument, valueType)
                .executes(context -> {
                    Hologram hologram = support.requireNamed(context);
                    if (hologram == null) {
                        return 0;
                    }
                    HologramPart part;
                    try {
                        part = factory.apply(StringArgumentType.getString(context, valueArgument));
                    } catch (IllegalArgumentException rejected) {
                        return reject(context, rejected);
                    }
                    support.plugin().config().applyCommonDefaults(part);
                    if (part instanceof TextPart text) {
                        support.plugin().config().applyDefaults(text);
                    }

                    int[] index = new int[1];
                    support.manager().restructure(hologram, () -> index[0] = hologram.addPart(part));
                    support.manager().requestSave();
                    support.messages().send(CommandSupport.senderOf(context), "part-added",
                            "name", hologram.name(), "index", String.valueOf(index[0]));
                    return Command.SINGLE_SUCCESS;
                });
        if (suggestions != null) {
            value.suggests(suggestions);
        }
        return Commands.literal(typeLiteral)
                .then(LifecycleNodes.named(support).then(value));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> remove(CommandSupport support) {
        return Commands.literal("remove")
                .then(LifecycleNodes.named(support)
                        .then(indexArgument(support).executes(context -> {
                            Target target = resolveTarget(context, support);
                            if (target == null) {
                                return 0;
                            }
                            Hologram hologram = target.hologram();
                            try {
                                support.manager().restructure(hologram,
                                        () -> hologram.removePart(target.partIndex()));
                            } catch (IllegalStateException lastPart) {
                                support.messages().send(CommandSupport.senderOf(context),
                                        "cannot-remove-last-part");
                                return 0;
                            }
                            support.manager().requestSave();
                            support.messages().send(CommandSupport.senderOf(context), "part-removed",
                                    "name", hologram.name(), "index", String.valueOf(target.partIndex()));
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> list(CommandSupport support) {
        return Commands.literal("list")
                .then(LifecycleNodes.named(support).executes(context -> {
                    Hologram hologram = support.requireNamed(context);
                    if (hologram == null) {
                        return 0;
                    }
                    CommandSender sender = CommandSupport.senderOf(context);
                    sender.sendMessage(Component.text(
                            hologram.name() + " has " + hologram.partCount() + " part(s)", NamedTextColor.AQUA));
                    for (int i = 0; i < hologram.partCount(); i++) {
                        HologramPart part = hologram.part(i);
                        sender.sendMessage(Component.text("#" + i + " " + part.type().id(), NamedTextColor.WHITE)
                                .append(Component.text(" " + QueryNodes.describePart(part), NamedTextColor.GRAY)));
                    }
                    return Command.SINGLE_SUCCESS;
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> offset(CommandSupport support) {
        return Commands.literal("offset")
                .then(LifecycleNodes.named(support)
                        .then(indexArgument(support)
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(context -> doOffset(context, support)))))));
    }

    private static int doOffset(CommandContext<CommandSourceStack> context, CommandSupport support) {
        Target target = resolveTarget(context, support);
        if (target == null) {
            return 0;
        }
        Vec3 offset = new Vec3(
                DoubleArgumentType.getDouble(context, "x"),
                DoubleArgumentType.getDouble(context, "y"),
                DoubleArgumentType.getDouble(context, "z"));
        // The offset decides where the entity is spawned, so it cannot be pushed to a live entity.
        Hologram hologram = target.hologram();
        support.manager().restructure(hologram,
                () -> hologram.part(target.partIndex()).offset(offset));
        support.manager().requestSave();
        support.messages().send(CommandSupport.senderOf(context), "updated",
                "attribute", "offset", "name", hologram.name());
        return Command.SINGLE_SUCCESS;
    }

    /** {@code /3deh part edit <name> <index> <attribute> ...} reuses the shared attribute branches. */
    private static LiteralArgumentBuilder<CommandSourceStack> edit(CommandSupport support) {
        var index = indexArgument(support);
        AttributeNodes.attach(index, support,
                context -> resolveTarget(context, support));
        return Commands.literal("edit").then(LifecycleNodes.named(support).then(index));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer>
            indexArgument(CommandSupport support) {
        return Commands.<Integer>argument(INDEX_ARGUMENT, IntegerArgumentType.integer(0))
                .suggests(Suggestions.partIndices(support.manager(), CommandSupport.NAME_ARGUMENT));
    }

    private static Target resolveTarget(CommandContext<CommandSourceStack> context, CommandSupport support) {
        return support.namedPart(context, IntegerArgumentType.getInteger(context, INDEX_ARGUMENT));
    }

    private static int reject(CommandContext<CommandSourceStack> context, IllegalArgumentException cause) {
        CommandSupport.senderOf(context).sendMessage(
                Component.text(cause.getMessage(), NamedTextColor.RED));
        return 0;
    }
}
