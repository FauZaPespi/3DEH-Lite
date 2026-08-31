package dev.fauza.tdeh.command.nodes;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.fauza.tdeh.command.CommandSupport;
import dev.fauza.tdeh.command.CommandSupport.PartTarget;
import dev.fauza.tdeh.command.CommandSupport.Target;
import dev.fauza.tdeh.command.args.Suggestions;
import dev.fauza.tdeh.model.BillboardMode;
import dev.fauza.tdeh.model.BlockPart;
import dev.fauza.tdeh.model.Brightness;
import dev.fauza.tdeh.model.EulerRotation;
import dev.fauza.tdeh.model.HologramPart;
import dev.fauza.tdeh.model.ItemPart;
import dev.fauza.tdeh.model.ItemTransform;
import dev.fauza.tdeh.model.TextAlign;
import dev.fauza.tdeh.model.TextPart;
import dev.fauza.tdeh.model.Vec3;
import dev.fauza.tdeh.text.ColorCodec;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds the attribute-editing branches shared by {@code /3deh edit} and {@code /3deh part edit}.
 *
 * <p>The same branches hang off three different parents, which differ only in how they work out
 * which part is being edited. That is what {@link PartTarget} abstracts, and why every builder here
 * is produced fresh on each call: attaching one already-built Brigadier node under two parents
 * makes the dispatcher share state between them.
 */
public final class AttributeNodes {

    private AttributeNodes() {
    }

    /** Hangs every attribute branch off {@code parent}, resolving the edited part through {@code target}. */
    public static void attach(ArgumentBuilder<CommandSourceStack, ?> parent,
                              CommandSupport support,
                              PartTarget target) {
        parent.then(scale(support, target));
        parent.then(rotation(support, target));
        parent.then(translation(support, target));
        parent.then(billboard(support, target));
        parent.then(viewRange(support, target));
        parent.then(shadow(support, target));
        parent.then(glow(support, target));
        parent.then(brightness(support, target));
        parent.then(size(support, target, "width", HologramPart::displayWidth));
        parent.then(size(support, target, "height", HologramPart::displayHeight));
        parent.then(interpolation(support, target));
        parent.then(teleportDuration(support, target));

        parent.then(text(support, target));
        parent.then(lineWidth(support, target));
        parent.then(opacity(support, target));
        parent.then(textFlag(support, target, "textshadow", TextPart::textShadow));
        parent.then(textFlag(support, target, "seethrough", TextPart::seeThrough));
        parent.then(background(support, target));
        parent.then(alignment(support, target));

        parent.then(block(support, target));
        parent.then(item(support, target));
        parent.then(itemTransform(support, target));
    }

    // ---------------------------------------------------------------- common attributes

    private static LiteralArgumentBuilder<CommandSourceStack> scale(CommandSupport support, PartTarget target) {
        return Commands.literal("scale")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg(0))
                        // One value scales uniformly, which is what an operator wants nine times in ten.
                        .executes(context -> apply(context, support, target, "scale",
                                part -> part.scale(Vec3.of(DoubleArgumentType.getDouble(context, "x")))))
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg(0))
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg(0))
                                        .executes(context -> apply(context, support, target, "scale",
                                                part -> part.scale(vec3(context)))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rotation(CommandSupport support, PartTarget target) {
        return Commands.literal("rotation")
                .then(Commands.argument("yaw", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("pitch", DoubleArgumentType.doubleArg())
                                .executes(context -> apply(context, support, target, "rotation",
                                        part -> part.rotation(new EulerRotation(
                                                DoubleArgumentType.getDouble(context, "yaw"),
                                                DoubleArgumentType.getDouble(context, "pitch"),
                                                0))))
                                .then(Commands.argument("roll", DoubleArgumentType.doubleArg())
                                        .executes(context -> apply(context, support, target, "rotation",
                                                part -> part.rotation(new EulerRotation(
                                                        DoubleArgumentType.getDouble(context, "yaw"),
                                                        DoubleArgumentType.getDouble(context, "pitch"),
                                                        DoubleArgumentType.getDouble(context, "roll"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> translation(CommandSupport support, PartTarget target) {
        return Commands.literal("translation")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(context -> apply(context, support, target, "translation",
                                                part -> part.translation(vec3(context)))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> billboard(CommandSupport support, PartTarget target) {
        return Commands.literal("billboard")
                .then(enumArgument("mode", BillboardMode.values())
                        .executes(context -> applyEnum(context, support, target, "billboard", "mode",
                                BillboardMode::fromId, HologramPart::billboard)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> viewRange(CommandSupport support, PartTarget target) {
        return Commands.literal("viewrange")
                .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0))
                        .executes(context -> apply(context, support, target, "viewrange",
                                part -> part.viewRange(
                                        (float) DoubleArgumentType.getDouble(context, "multiplier")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> shadow(CommandSupport support, PartTarget target) {
        return Commands.literal("shadow")
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0))
                        .then(Commands.argument("strength", DoubleArgumentType.doubleArg(0))
                                .executes(context -> apply(context, support, target, "shadow", part -> {
                                    part.shadowRadius((float) DoubleArgumentType.getDouble(context, "radius"));
                                    part.shadowStrength((float) DoubleArgumentType.getDouble(context, "strength"));
                                }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> glow(CommandSupport support, PartTarget target) {
        return Commands.literal("glow")
                .then(colorArgument("colour")
                        .executes(context -> {
                            String raw = StringArgumentType.getString(context, "colour");
                            if (ColorCodec.isNone(raw)) {
                                return apply(context, support, target, "glow", part -> part.glowColor(null));
                            }
                            Optional<Integer> rgb = ColorCodec.parse(raw);
                            if (rgb.isEmpty()) {
                                return badValue(context, support, raw, "a colour such as #ff0055, red, or none");
                            }
                            return apply(context, support, target, "glow",
                                    part -> part.glowColor(rgb.get()));
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> brightness(CommandSupport support, PartTarget target) {
        return Commands.literal("brightness")
                .then(Commands.literal("none")
                        .executes(context -> apply(context, support, target, "brightness",
                                part -> part.brightness(null))))
                .then(Commands.argument("block", IntegerArgumentType.integer(
                                Brightness.MIN_LEVEL, Brightness.MAX_LEVEL))
                        .then(Commands.argument("sky", IntegerArgumentType.integer(
                                        Brightness.MIN_LEVEL, Brightness.MAX_LEVEL))
                                .executes(context -> apply(context, support, target, "brightness",
                                        part -> part.brightness(new Brightness(
                                                IntegerArgumentType.getInteger(context, "block"),
                                                IntegerArgumentType.getInteger(context, "sky")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> size(CommandSupport support,
                                                                  PartTarget target,
                                                                  String label,
                                                                  SizeSetter setter) {
        return Commands.literal(label)
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                        .executes(context -> apply(context, support, target, label,
                                part -> setter.set(part, (float) DoubleArgumentType.getDouble(context, "value")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> interpolation(CommandSupport support, PartTarget target) {
        return Commands.literal("interpolation")
                .then(Commands.argument("delay", IntegerArgumentType.integer(0))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                .executes(context -> apply(context, support, target, "interpolation", part -> {
                                    part.interpolationDelay(IntegerArgumentType.getInteger(context, "delay"));
                                    part.interpolationDuration(IntegerArgumentType.getInteger(context, "duration"));
                                }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> teleportDuration(CommandSupport support,
                                                                              PartTarget target) {
        return Commands.literal("teleportduration")
                .then(Commands.argument("ticks", IntegerArgumentType.integer(
                                HologramPart.MIN_TELEPORT_DURATION, HologramPart.MAX_TELEPORT_DURATION))
                        .executes(context -> apply(context, support, target, "teleportduration",
                                part -> part.teleportDuration(
                                        IntegerArgumentType.getInteger(context, "ticks")))));
    }

    // ---------------------------------------------------------------- text attributes

    private static LiteralArgumentBuilder<CommandSourceStack> text(CommandSupport support, PartTarget target) {
        return Commands.literal("text")
                .then(Commands.argument("value", StringArgumentType.greedyString())
                        .executes(context -> applyTo(context, support, target, "text", TextPart.class,
                                part -> part.text(StringArgumentType.getString(context, "value")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> lineWidth(CommandSupport support, PartTarget target) {
        return Commands.literal("linewidth")
                .then(Commands.argument("pixels", IntegerArgumentType.integer(1))
                        .executes(context -> applyTo(context, support, target, "linewidth", TextPart.class,
                                part -> part.lineWidth(IntegerArgumentType.getInteger(context, "pixels")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> opacity(CommandSupport support, PartTarget target) {
        return Commands.literal("opacity")
                .then(Commands.argument("value", IntegerArgumentType.integer(
                                TextPart.DEFAULT_OPACITY, TextPart.MAX_OPACITY))
                        .executes(context -> applyTo(context, support, target, "opacity", TextPart.class,
                                part -> part.opacity(IntegerArgumentType.getInteger(context, "value")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> textFlag(CommandSupport support,
                                                                      PartTarget target,
                                                                      String label,
                                                                      TextFlagSetter setter) {
        return Commands.literal(label)
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> applyTo(context, support, target, label, TextPart.class,
                                part -> setter.set(part, BoolArgumentType.getBool(context, "enabled")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> background(CommandSupport support, PartTarget target) {
        return Commands.literal("background")
                // "default" restores the vanilla translucent box; "none" makes it fully transparent.
                .then(Commands.literal("default")
                        .executes(context -> applyTo(context, support, target, "background", TextPart.class,
                                part -> {
                                    part.backgroundColor(null);
                                    part.defaultBackground(true);
                                })))
                .then(Commands.literal("none")
                        .executes(context -> applyTo(context, support, target, "background", TextPart.class,
                                part -> {
                                    part.backgroundColor(null);
                                    part.defaultBackground(false);
                                })))
                .then(colorArgument("colour")
                        .executes(context -> {
                            String raw = StringArgumentType.getString(context, "colour");
                            Optional<Integer> argb = ColorCodec.parseArgb(raw);
                            if (argb.isEmpty()) {
                                return badValue(context, support, raw,
                                        "a colour such as #80112233, #ff0055, red, none, or default");
                            }
                            return applyTo(context, support, target, "background", TextPart.class,
                                    part -> part.backgroundColor(argb.get()));
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> alignment(CommandSupport support, PartTarget target) {
        return Commands.literal("alignment")
                .then(enumArgument("mode", TextAlign.values())
                        .executes(context -> {
                            String raw = StringArgumentType.getString(context, "mode");
                            Optional<TextAlign> value = TextAlign.fromId(raw);
                            if (value.isEmpty()) {
                                return badValue(context, support, raw, "one of center, left, right");
                            }
                            return applyTo(context, support, target, "alignment", TextPart.class,
                                    part -> part.alignment(value.get()));
                        }));
    }

    // ---------------------------------------------------------------- block and item attributes

    private static LiteralArgumentBuilder<CommandSourceStack> block(CommandSupport support, PartTarget target) {
        return Commands.literal("block")
                .then(Commands.argument("data", StringArgumentType.greedyString())
                        .suggests(Suggestions.blockMaterials())
                        .executes(context -> applyTo(context, support, target, "block", BlockPart.class,
                                part -> part.blockData(StringArgumentType.getString(context, "data")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> item(CommandSupport support, PartTarget target) {
        return Commands.literal("item")
                .then(Commands.argument("material", StringArgumentType.word())
                        .suggests(Suggestions.itemMaterials())
                        .executes(context -> applyTo(context, support, target, "item", ItemPart.class,
                                part -> part.material(StringArgumentType.getString(context, "material")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> itemTransform(CommandSupport support, PartTarget target) {
        return Commands.literal("itemtransform")
                .then(enumArgument("mode", ItemTransform.values())
                        .executes(context -> {
                            String raw = StringArgumentType.getString(context, "mode");
                            Optional<ItemTransform> value = ItemTransform.fromId(raw);
                            if (value.isEmpty()) {
                                return badValue(context, support, raw, "an item display transform");
                            }
                            return applyTo(context, support, target, "itemtransform", ItemPart.class,
                                    part -> part.transform(value.get()));
                        }));
    }

    // ---------------------------------------------------------------- application

    /** Applies an edit that is valid for any part type. */
    private static int apply(CommandContext<CommandSourceStack> context,
                             CommandSupport support,
                             PartTarget target,
                             String attribute,
                             Consumer<HologramPart> edit) {
        return applyTo(context, support, target, attribute, HologramPart.class, edit);
    }

    /**
     * Applies an edit that only makes sense for one part type, reporting the mismatch by name rather
     * than silently doing nothing when, say, {@code linewidth} is aimed at an item part.
     */
    private static <T extends HologramPart> int applyTo(CommandContext<CommandSourceStack> context,
                                                        CommandSupport support,
                                                        PartTarget target,
                                                        String attribute,
                                                        Class<T> expected,
                                                        Consumer<T> edit) {
        Target resolved = target.resolve(context);
        if (resolved == null) {
            return 0;
        }
        HologramPart part = resolved.hologram().part(resolved.partIndex());
        if (!expected.isInstance(part)) {
            support.messages().send(CommandSupport.senderOf(context), "wrong-part-type",
                    "attribute", attribute,
                    "expected", labelOf(expected),
                    "index", String.valueOf(resolved.partIndex()),
                    "actual", part.type().id());
            return 0;
        }
        try {
            support.manager().editPart(resolved.hologram(), resolved.partIndex(),
                    generic -> edit.accept(expected.cast(generic)));
        } catch (IllegalArgumentException rejected) {
            CommandSupport.senderOf(context).sendMessage(
                    Component.text(rejected.getMessage(), NamedTextColor.RED));
            return 0;
        }
        support.manager().requestSave();
        support.messages().send(CommandSupport.senderOf(context), "updated",
                "attribute", attribute, "name", resolved.hologram().name());
        return Command.SINGLE_SUCCESS;
    }

    /** Shared tail for enum attributes that apply to every part type. */
    private static <E extends Enum<E>> int applyEnum(CommandContext<CommandSourceStack> context,
                                                     CommandSupport support,
                                                     PartTarget target,
                                                     String attribute,
                                                     String argument,
                                                     Function<String, Optional<E>> parser,
                                                     EnumSetter<E> setter) {
        String raw = StringArgumentType.getString(context, argument);
        Optional<E> value = parser.apply(raw);
        if (value.isEmpty()) {
            return badValue(context, support, raw, "a valid " + attribute + " mode");
        }
        return apply(context, support, target, attribute, part -> setter.set(part, value.get()));
    }

    private static int badValue(CommandContext<CommandSourceStack> context,
                                CommandSupport support,
                                String raw,
                                String expected) {
        CommandSupport.senderOf(context).sendMessage(
                Component.text("'" + raw + "' is not " + expected + ".", NamedTextColor.RED));
        return 0;
    }

    private static String labelOf(Class<? extends HologramPart> type) {
        if (type == TextPart.class) {
            return "text";
        }
        if (type == BlockPart.class) {
            return "block";
        }
        if (type == ItemPart.class) {
            return "item";
        }
        return "any";
    }

    // ---------------------------------------------------------------- argument helpers

    private static Vec3 vec3(CommandContext<CommandSourceStack> context) {
        return new Vec3(
                DoubleArgumentType.getDouble(context, "x"),
                DoubleArgumentType.getDouble(context, "y"),
                DoubleArgumentType.getDouble(context, "z"));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> enumArgument(String name, Enum<?>[] values) {
        return Commands.<String>argument(name, StringArgumentType.word())
                .suggests((context, builder) -> {
                    String prefix = builder.getRemainingLowerCase();
                    for (Enum<?> value : values) {
                        String id = value.name().toLowerCase(Locale.ROOT);
                        if (id.startsWith(prefix)) {
                            builder.suggest(id);
                        }
                    }
                    return builder.buildFuture();
                });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> colorArgument(String name) {
        ArgumentType<String> type = StringArgumentType.word();
        return Commands.<String>argument(name, type)
                .suggests((context, builder) -> {
                    for (String suggestion : new String[]{"none", "#ff0055", "white", "red", "aqua", "gold"}) {
                        if (suggestion.startsWith(builder.getRemainingLowerCase())) {
                            builder.suggest(suggestion);
                        }
                    }
                    return builder.buildFuture();
                });
    }

    @FunctionalInterface
    private interface SizeSetter {
        void set(HologramPart part, float value);
    }

    @FunctionalInterface
    private interface TextFlagSetter {
        void set(TextPart part, boolean value);
    }

    @FunctionalInterface
    private interface EnumSetter<E> {
        void set(HologramPart part, E value);
    }
}
