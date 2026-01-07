package com.olziedev.antibackteleport;

import com.olziedev.antibackteleport.manager.AddonManager;
import com.olziedev.antibackteleport.utils.Configuration;
import com.olziedev.antibackteleport.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.OlzieCommand;
import com.olziedev.olziecommand.v1_3_3.framework.action.CommandActionType;
import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;

public class AntiBackTeleport extends SpotPlugin {

    private static AntiBackTeleport instance = null;

    private AddonManager addonManager;

    @Override
    public String getName() {
        return "AntiBackTeleport";
    }

    @Override
    public void onEnable() {
        instance = this;
        new Configuration(this).load(); // load all the configuration files in memory

        this.addonManager = new AddonManager(this);
        this.addonManager.setup();
        this.addonManager.load();

        new OlzieCommand(this.plugin, getClass())
                .getActionRegister()
                .registerAction(CommandActionType.CMD_NO_PERMISSION, cmd -> {
                    Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.no-permission"));
                }).buildActions()
                .registerCommands(); // automatically register commands
    }

    @Override
    public void onDisable() {
        this.addonManager.close();
        this.addonManager = null;

        Bukkit.getScheduler().cancelTasks(this.plugin);
        instance = null;
    }

    public static AntiBackTeleport getInstance() {
        return instance;
    }

    public static AddonManager getAddonManager() {
        return instance.addonManager;
    }
}
