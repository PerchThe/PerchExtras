package com.olziedev.spotextras;

import com.olziedev.antibackteleport.AntiBackTeleport;
import com.olziedev.inactivediscordroles.InactiveDiscordRoles;
import com.olziedev.potion.Potion;
import com.olziedev.realestate.RealEstate;
import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpotExtras extends JavaPlugin {

    private static SpotExtras instance;

    private List<SpotPlugin> plugins;

    @Override
    public void onEnable() {
        plugins = new ArrayList<>();
        instance = this;
        plugins.addAll(Arrays.asList(new RealEstate(), new Potion(), new InactiveDiscordRoles(), new AntiBackTeleport()));
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
        for (SpotPlugin plugin : plugins) {
            try {
                this.getLogger().info("Disabling " + plugin.getName());
                plugin.onDisable();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
        instance = null;
    }

    public static JavaPlugin getInstance() {
        return instance;
    }
}
