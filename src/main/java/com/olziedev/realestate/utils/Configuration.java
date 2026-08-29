package com.olziedev.realestate.utils;

import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

public class Configuration {

    private final SpotPlugin plugin;
    private static FileConfiguration config;
    private static FileConfiguration gui;

    public Configuration(SpotPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            File dataFolder = plugin.getDataFolder();
            load(new File(dataFolder, "config.yml"), getClass().getDeclaredField("config"));
            load(new File(dataFolder, "guis.yml"), getClass().getDeclaredField("gui"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void load(File file, Field field) throws Exception {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            plugin.saveResource(file.getParentFile().getName() + File.separator + file.getName(), false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        String resourcePath = file.getParentFile().getName() + "/" + file.getName();
        try (InputStream resource = plugin.plugin.getResource(resourcePath)) {
            if (resource != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(resource, StandardCharsets.UTF_8)
                );
                loaded.setDefaults(defaults);
            }
        }
        field.set(null, loaded);
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static FileConfiguration getGUI() {
        return gui;
    }

    public static String getString(ConfigurationSection section, String s) {
        if (section == null) return "";

        return section.getString(s, "");
    }

    public static String getString(ConfigurationSection section, String path, String fallback) {
        if (section == null) return fallback;

        String value = section.getString(path, fallback);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String getString(YamlConfiguration config, String s) {
        return config.getString(s, "");
    }
}
