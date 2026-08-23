package com.olziedev.bulkmapart;

import com.olziedev.spotextras.api.SpotPlugin;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class Messages {

    private static final String RESOURCE_PATH = "bulkmapart/messages.yml";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final SpotPlugin module;
    private final File file;
    private YamlConfiguration configuration;
    private Component prefix;

    Messages(SpotPlugin module) {
        this.module = module;
        this.file = new File(module.getDataFolder(), "messages.yml");
    }

    void loadInitial() throws IOException, InvalidConfigurationException {
        if (!file.exists()) {
            module.saveResource(RESOURCE_PATH, false);
        }
        reload();
    }

    void reload() throws IOException, InvalidConfigurationException {
        YamlConfiguration defaults = loadBundledDefaults();
        YamlConfiguration loaded = new YamlConfiguration();
        loaded.options().parseComments(true);
        loaded.load(file);
        loaded.setDefaults(defaults);

        Component loadedPrefix = MINI_MESSAGE.deserialize(loaded.getString("prefix", ""));
        configuration = loaded;
        prefix = loadedPrefix;
    }

    void send(Audience audience, String key, TagResolver... placeholders) {
        audience.sendMessage(render(key, placeholders));
    }

    static TagResolver.Single value(String name, Object value) {
        return Placeholder.unparsed(name, String.valueOf(value));
    }

    static TagResolver.Single component(String name, Component value) {
        return Placeholder.component(name, value);
    }

    Component render(String key, TagResolver... placeholders) {
        String raw = configuration.getString(key, "<red>Missing message: " + key + "</red>");
        TagResolver.Builder resolver = TagResolver.builder()
                .resolver(Placeholder.component("prefix", prefix));
        for (TagResolver placeholder : placeholders) {
            resolver.resolver(placeholder);
        }
        return MINI_MESSAGE.deserialize(raw, resolver.build());
    }

    private YamlConfiguration loadBundledDefaults() throws IOException {
        try (InputStream stream = module.plugin.getResource(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IOException("The bundled " + RESOURCE_PATH + " file is missing");
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        }
    }
}
