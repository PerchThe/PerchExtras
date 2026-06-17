package com.olziedev.spotextras;

import com.olziedev.antibackteleport.AntiBackTeleport;
import com.olziedev.potion.Potion;
import com.olziedev.realestate.RealEstate;
import com.olziedev.spotextras.api.SpotPlugin;


import com.olziedev.lapis.LapisListener;
import com.olziedev.lapis.TableManager;
import com.olziedev.runcommandall.RunAllCommand;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpotExtras extends JavaPlugin {

    private static SpotExtras instance;
    private List<SpotPlugin> plugins;

    // Lapis tracking variables
    private NamespacedKey lapisKey;
    private TableManager tableManager;

    @Override
    public void onEnable() {
        instance = this;

        // --- 3. INITIALIZE THE LAPIS SYSTEM ---
        this.lapisKey = new NamespacedKey(this, "stored_lapis");
        this.tableManager = new TableManager();
        getServer().getPluginManager().registerEvents(new LapisListener(this, tableManager), this);

        // --- 4. INITIALIZE COMMANDS ---
        if (getCommand("runcommandall") != null) {
            getCommand("runcommandall").setExecutor(new RunAllCommand(this));
        } else {
            getLogger().warning("Could not find 'runcommandall' in plugin.yml!");
        }

        // --- 5. INITIALIZE SPOT MODULES ---
        plugins = new ArrayList<>();
        plugins.addAll(Arrays.asList(new RealEstate(), new Potion(), new AntiBackTeleport()));

        for (SpotPlugin plugin : plugins) {
            try {
                this.getLogger().info("Enabling " + plugin.getName());
                plugin.onEnable();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        // 1. Disable Spot Modules
        for (SpotPlugin plugin : plugins) {
            try {
                this.getLogger().info("Disabling " + plugin.getName());
                plugin.onDisable();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        // 2. Clear the Lapis tables to prevent ghost locks on reload
        if (tableManager != null) {
            tableManager.clearAll();
        }

        instance = null;
    }

    public static SpotExtras getInstance() {
        return instance;
    }

    // --- LAPIS GETTERS ---
    public NamespacedKey getLapisKey() {
        return lapisKey;
    }

    public TableManager getTableManager() {
        return tableManager;
    }
}