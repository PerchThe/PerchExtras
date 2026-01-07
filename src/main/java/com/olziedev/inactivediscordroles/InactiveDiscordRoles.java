package com.olziedev.inactivediscordroles;

import com.olziedev.inactivediscordroles.managers.AddonManager;
import com.olziedev.inactivediscordroles.managers.RoleManager;
import com.olziedev.inactivediscordroles.utils.Configuration;
import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;

public class InactiveDiscordRoles extends SpotPlugin {

    private static InactiveDiscordRoles instance = null;

    private AddonManager addonManager;
    private RoleManager roleManager;

    @Override
    public String getName() {
        return "InactiveDiscordRoles";
    }

    @Override
    public void onEnable() {
        instance = this;
        new Configuration(this).load(); // load all the configuration files in memory

        this.addonManager = new AddonManager(this);
        this.addonManager.setup();
        this.addonManager.load();

        this.roleManager = new RoleManager(this);
        this.roleManager.setup();
        this.roleManager.load();
    }

    @Override
    public void onDisable() {
        this.roleManager.close();
        this.roleManager = null;

        this.addonManager.close();
        this.addonManager = null;

        Bukkit.getScheduler().cancelTasks(this.plugin);
        instance = null;
    }

    public static InactiveDiscordRoles getInstance() {
        return instance;
    }

    public static RoleManager getRoleManager() {
        return instance.roleManager;
    }

    public static AddonManager getAddonManager() {
        return instance.addonManager;
    }
}
