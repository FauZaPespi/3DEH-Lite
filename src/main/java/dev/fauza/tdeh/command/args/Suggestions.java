package dev.fauza.tdeh.command.args;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.fauza.tdeh.manager.HologramManager;
import dev.fauza.tdeh.model.Hologram;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Suggestion providers for the arguments Brigadier cannot infer on its own.
 *
 * <p>All of them filter on the text typed so far. Without that, a server with a few hundred
 * holograms or the full material list would flood the client's completion popup.
 */
public final class Suggestions {

    private Suggestions() {
    }

    public static SuggestionProvider<CommandSourceStack> hologramNames(HologramManager manager) {
        return (context, builder) -> {
            String prefix = builder.getRemainingLowerCase();
            for (String name : manager.names()) {
                if (name.startsWith(prefix)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

    /** Items only: a {@code BlockDisplay}-only material would silently render as nothing. */
    public static SuggestionProvider<CommandSourceStack> itemMaterials() {
        return (context, builder) -> materials(builder, Material::isItem);
    }

    public static SuggestionProvider<CommandSourceStack> blockMaterials() {
        return (context, builder) -> materials(builder, Material::isBlock);
    }

    /** Part indices of the hologram already named earlier in the command line. */
    public static SuggestionProvider<CommandSourceStack> partIndices(HologramManager manager, String nameArgument) {
        return (context, builder) -> {
            Hologram hologram = hologramFrom(context, manager, nameArgument);
            if (hologram != null) {
                for (int i = 0; i < hologram.partCount(); i++) {
                    builder.suggest(i);
                }
            }
            return builder.buildFuture();
        };
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> materials(
            SuggestionsBuilder builder, java.util.function.Predicate<Material> filter) {
        String prefix = builder.getRemainingLowerCase();
        for (Material material : Material.values()) {
            if (material.isLegacy() || !filter.test(material)) {
                continue;
            }
            String name = material.getKey().getKey();
            if (name.startsWith(prefix)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    /**
     * Reading an earlier argument throws while that argument is still being typed, which is exactly
     * when suggestions run, so the failure is expected rather than exceptional.
     */
    private static Hologram hologramFrom(CommandContext<CommandSourceStack> context,
                                         HologramManager manager,
                                         String nameArgument) {
        try {
            String name = context.getArgument(nameArgument, String.class);
            return manager.find(name).orElse(null);
        } catch (IllegalArgumentException notYetTyped) {
            return null;
        }
    }

    public static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
