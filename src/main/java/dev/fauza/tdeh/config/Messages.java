package dev.fauza.tdeh.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Map;

/**
 * Renders the operator-facing strings from {@code config.yml}.
 *
 * <p>Placeholders are passed as alternating name/value pairs and resolved with
 * {@code Placeholder.unparsed}, so a hologram named {@code <red>} cannot smuggle formatting into a
 * feedback message.
 */
public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Map<String, String> messages;
    private final String prefix;

    Messages(Map<String, String> messages, String prefix) {
        this.messages = Map.copyOf(messages);
        this.prefix = prefix;
    }

    public Component render(String key, String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be name/value pairs, got " + placeholders.length);
        }
        String template = messages.get(key);
        if (template == null) {
            // A missing key is a packaging bug, not something to hide from whoever ran the command.
            return Component.text("Missing message: " + key);
        }
        TagResolver.Builder resolvers = TagResolver.builder();
        for (int i = 0; i < placeholders.length; i += 2) {
            resolvers.resolver(Placeholder.unparsed(placeholders[i], placeholders[i + 1]));
        }
        return MINI_MESSAGE.deserialize(prefix + template, resolvers.build());
    }

    public void send(Audience audience, String key, String... placeholders) {
        audience.sendMessage(render(key, placeholders));
    }

    /** Renders without the plugin prefix, for multi-line output such as help and list pages. */
    public Component renderBare(String key, String... placeholders) {
        String template = messages.get(key);
        if (template == null) {
            return Component.text("Missing message: " + key);
        }
        TagResolver.Builder resolvers = TagResolver.builder();
        for (int i = 0; i < placeholders.length; i += 2) {
            resolvers.resolver(Placeholder.unparsed(placeholders[i], placeholders[i + 1]));
        }
        return MINI_MESSAGE.deserialize(template, resolvers.build());
    }
}
