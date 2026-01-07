package com.olziedev.potion;

import com.olziedev.olziecommand.v1_3_3.OlzieCommand;
import com.olziedev.olziecommand.v1_3_3.framework.action.CommandActionType;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import com.olziedev.potion.commands.MilkCommand;
import com.olziedev.potion.commands.PotionCommand;
import com.olziedev.potion.events.JoinEvent;
import com.olziedev.potion.managers.DatabaseManager;
import com.olziedev.potion.utils.Configuration;
import com.olziedev.potion.utils.Utils;
import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Potion extends SpotPlugin {

    private static Potion instance = null;

    private DatabaseManager databaseManager;

    @Override
    public String getName() {
        return "Potion";
    }

    @Override
    public void onEnable() {
        instance = this;
        new Configuration(this).load(); // load all the configuration files in memory

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.setup();
        this.databaseManager.load();

        OlzieCommand olzieCommand = new OlzieCommand(this.plugin, getClass())
                .getActionRegister()
                .registerAction(CommandActionType.CMD_NO_PERMISSION, cmd -> {
                    Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.no-permission"));
                }).buildActions();
        List<FrameworkCommand> commands = new ArrayList<>();
        commands.add(new MilkCommand());

        ConfigurationSection section = Configuration.getConfig().getConfigurationSection("settings.potions");
        for (String s : section == null ? Collections.<String>emptyList() : section.getKeys(false)) {
            PotionEffectType potionType = PotionEffectType.getByName(s);
            if (potionType == null) {
                this.getLogger().info(s + " is an invalid potion!");
                continue;
            }
            commands.add(new PotionCommand(potionType, section.getString(s + ".command")));
            databaseManager.getPotions().put(potionType.getName(), potionType);
        }
        olzieCommand.registerCommands(commands);
        Bukkit.getPluginManager().registerEvents(new JoinEvent(), this.plugin);
    }

    @Override
    public void onDisable() {
        this.databaseManager.close();
        this.databaseManager = null;

        Bukkit.getScheduler().cancelTasks(this.plugin);
        instance = null;
    }

    public static Potion getInstance() {
        return instance;
    }

    public static DatabaseManager getDatabaseManager() {
        return instance.databaseManager;
    }
}
