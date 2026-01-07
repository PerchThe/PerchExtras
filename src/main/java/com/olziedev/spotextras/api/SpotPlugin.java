package com.olziedev.spotextras.api;

import com.olziedev.spotextras.SpotExtras;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public abstract class SpotPlugin {

    public final JavaPlugin plugin = SpotExtras.getInstance();
    private final Logger logger = LogManager.getLogger(plugin.getName() + " - " + getName());

    public abstract String getName();

    public abstract void onEnable();

    public abstract void onDisable();

    public final File getDataFolder() {
        return new File(plugin.getDataFolder() + File.separator + getName());
    }

    public void saveResource(String name, boolean b) {
        plugin.saveResource(name, b);
    }

    public Logger getLogger() {
        return logger;
    }
}
