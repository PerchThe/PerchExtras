package com.olziedev.realestate;

import com.olziedev.olziecommand.v1_3_3.OlzieCommand;
import com.olziedev.olziecommand.v1_3_3.framework.action.CommandActionType;
import com.olziedev.realestate.estate.rent.RentTimer;
import com.olziedev.realestate.events.BreakEvent;
import com.olziedev.realestate.events.InteractEvent;
import com.olziedev.realestate.events.JoinEvent;
import com.olziedev.realestate.events.PlaceEvent;
import com.olziedev.realestate.managers.AddonManager;
import com.olziedev.realestate.managers.DatabaseManager;
import com.olziedev.realestate.managers.MenuManager;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;

public class RealEstate extends SpotPlugin {

    private static RealEstate instance = null;

    private DatabaseManager databaseManager;
    private AddonManager addonManager;
    private MenuManager menuManager;

    @Override
    public String getName() {
        return "RealEstate";
    }

    @Override
    public void onEnable() {
        instance = this;
        new Configuration(this).load(); // load all the configuration files in memory

        this.addonManager = new AddonManager(this);
        this.addonManager.setup();
        this.addonManager.load();

        this.menuManager = new MenuManager(this);
        this.menuManager.setup();
        this.menuManager.load();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.setup();
        this.databaseManager.load();

        new OlzieCommand(this.plugin, getClass())
                .getActionRegister()
                .registerAction(CommandActionType.CMD_NO_PERMISSION, cmd -> {
                    Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.no-permission"));
                })
                .registerAction(CommandActionType.CMD_NOT_PLAYER, cmd -> {
                    Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.player-only"));
                }).buildActions()
                .registerCommands(); // automatically register commands

        Bukkit.getPluginManager().registerEvents(new InteractEvent(), plugin);
        Bukkit.getPluginManager().registerEvents(new PlaceEvent(), plugin);
        Bukkit.getPluginManager().registerEvents(new BreakEvent(), plugin);
        Bukkit.getPluginManager().registerEvents(new JoinEvent(), plugin);
        new RentTimer().runTaskTimerAsynchronously(plugin, 0, 20 * 30);
    }

    @Override
    public void onDisable() {
        this.databaseManager.close();
        this.databaseManager = null;

        this.addonManager.close();
        this.addonManager = null;

        this.menuManager.close();
        this.menuManager = null;

        Bukkit.getScheduler().cancelTasks(plugin);
        instance = null;
    }

    public static RealEstate getInstance() {
        return instance;
    }

    public static DatabaseManager getDatabaseManager() {
        return instance.databaseManager;
    }

    public static MenuManager getMenuManager() {
        return instance.menuManager;
    }

    public static AddonManager getAddonManager() {
        return instance.addonManager;
    }
}
