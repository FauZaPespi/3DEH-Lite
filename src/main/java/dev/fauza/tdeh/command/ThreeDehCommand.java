package dev.fauza.tdeh.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.fauza.tdeh.ThreeDehPlugin;
import dev.fauza.tdeh.command.nodes.AttributeNodes;
import dev.fauza.tdeh.command.nodes.LifecycleNodes;
import dev.fauza.tdeh.command.nodes.PartNodes;
import dev.fauza.tdeh.command.nodes.QueryNodes;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.List;

/**
 * Builds and registers the {@code /3deh} command tree.
 *
 * <p>Registration goes through the lifecycle manager rather than {@code plugin.yml}, which is why
 * the plugin descriptor declares no {@code commands:} section. Brigadier then provides completion,
 * argument validation and syntax errors, and {@code .requires(...)} hides branches the sender has no
 * permission for instead of the plugin filtering suggestions by hand.
 */
public final class ThreeDehCommand {

    private static final String ROOT = "3deh";

    private final ThreeDehPlugin plugin;
    private final CommandSupport support;

    public ThreeDehCommand(ThreeDehPlugin plugin) {
        this.plugin = plugin;
        this.support = new CommandSupport(plugin);
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        build().build(),
                        "Lightweight holograms built on native display entities.",
                        List.of("3dehologram")));
    }

    private LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT)
                // Bare /3deh behaves as /3deh help, which is what people try first.
                .executes(QueryNodes.helpExecutor(support));

        root.then(QueryNodes.help(support));
        root.then(QueryNodes.list(support));
        root.then(QueryNodes.info(support));
        root.then(QueryNodes.select(support));
        root.then(QueryNodes.deselect(support));

        root.then(LifecycleNodes.create(support));
        root.then(LifecycleNodes.remove(support));
        root.then(LifecycleNodes.move(support));
        root.then(LifecycleNodes.teleport(support));
        root.then(LifecycleNodes.reload(support));

        root.then(PartNodes.part(support));
        root.then(edit());
        return root;
    }

    /**
     * {@code /3deh edit} accepts the hologram name or leans on the player's selection.
     *
     * <p>Brigadier tries literal children before argument children, so {@code /3deh edit scale 2}
     * matches the attribute branch while {@code /3deh edit shop scale 2} falls through to the name
     * argument. The attribute subtree is built twice from scratch rather than shared, because
     * attaching one built node under two parents makes the dispatcher entangle their state.
     */
    private LiteralArgumentBuilder<CommandSourceStack> edit() {
        LiteralArgumentBuilder<CommandSourceStack> edit = Commands.literal("edit")
                .requires(source -> source.getSender().hasPermission("3deh.command.edit"));

        AttributeNodes.attach(edit, support, support::selectedFirstPart);

        var named = LifecycleNodes.named(support);
        AttributeNodes.attach(named, support, support::namedFirstPart);
        edit.then(named);
        return edit;
    }
}
