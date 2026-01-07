package com.olziedev.notes;

import com.olziedev.notes.managers.DatabaseManager;
import com.olziedev.notes.utils.Configuration;
import com.olziedev.notes.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.OlzieCommand;
import com.olziedev.olziecommand.v1_3_3.framework.action.CommandActionType;
import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;

public class Notes extends SpotPlugin {

    private static Notes instance = null;

    private DatabaseManager databaseManager;

    @Override
    public String getName() {
        return "Notes";
    }

    @Override
    public void onEnable() {
        instance = this;
        new Configuration(this).load(); // load all the configuration files in memory

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
    }

    @Override
    public void onDisable() {
        this.databaseManager.close();
        this.databaseManager = null;

        Bukkit.getScheduler().cancelTasks(plugin);
        instance = null;
    }

    public static Notes getInstance() {
        return instance;
    }

    public static DatabaseManager getDatabaseManager() {
        return instance.databaseManager;
    }
}
