package com.olziedev.invisibleitemframes;

import com.olziedev.invisibleitemframes.events.InteractEvent;
import com.olziedev.invisibleitemframes.events.JoinEvent;
import com.olziedev.invisibleitemframes.managers.DatabaseManager;
import com.olziedev.invisibleitemframes.managers.FrameManager;
import com.olziedev.invisibleitemframes.utils.Configuration;
import com.olziedev.invisibleitemframes.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.OlzieCommand;
import com.olziedev.olziecommand.v1_3_3.framework.action.CommandActionType;
import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;

public class InvisibleItemFrames extends SpotPlugin {

    private static InvisibleItemFrames instance = null;

    private DatabaseManager databaseManager;
    private FrameManager frameManager;

    @Override
    public String getName() {
        return "InvisibleItemFrames";
    }

    @Override
    public void onEnable() {
        instance = this;
        new Configuration(this).load(); // load all the configuration files in memory

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.setup();
        this.databaseManager.load();

        this.frameManager = new FrameManager(this);
        this.frameManager.setup();
        this.frameManager.load();

        new OlzieCommand(this.plugin, getClass())
                .getActionRegister()
                .registerAction(CommandActionType.CMD_NO_PERMISSION, cmd -> {
                    Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.no-permission"));
                }).buildActions()
                .registerCommands();
        Bukkit.getPluginManager().registerEvents(new InteractEvent(), this.plugin);
        Bukkit.getPluginManager().registerEvents(new JoinEvent(), this.plugin);
    }

    @Override
    public void onDisable() {
        this.databaseManager.close();
        this.databaseManager = null;

        this.frameManager.close();
        this.frameManager = null;

        Bukkit.getScheduler().cancelTasks(this.plugin);
        instance = null;
    }

    public static InvisibleItemFrames getInstance() {
        return instance;
    }

    public static DatabaseManager getDatabaseManager() {
        return instance.databaseManager;
    }

    public static FrameManager getFrameManager() {
        return instance.frameManager;
    }

}
