package com.olziedev.potion;

import com.olziedev.olziecommand.v1_3_3.OlzieCommand;
import com.olziedev.olziecommand.v1_3_3.framework.action.CommandActionType;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import com.olziedev.potion.commands.MilkCommand;
import com.olziedev.potion.commands.PotionCommand;
import com.olziedev.potion.listeners.PotionListener;
import com.olziedev.potion.utils.Configuration;
import com.olziedev.potion.utils.Utils;
import com.olziedev.spotextras.api.SpotPlugin;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Potion extends SpotPlugin {

    private static Potion instance = null;

    public static NamespacedKey POTION_KEY;

    @Override
    public String getName() {
        return "Potion";
    }

    @Override
    public void onEnable() {
        instance = this;
        POTION_KEY = new NamespacedKey(this.plugin, "saved_potions");
        new Configuration(this).load();
        Bukkit.getPluginManager().registerEvents(new PotionListener(), this.plugin);

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
        }
        olzieCommand.registerCommands(commands);
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this.plugin);
        instance = null;
    }

    public static Potion getInstance() {
        return instance;
    }

    public JavaPlugin getPlugin() {
        return (JavaPlugin) this.plugin;
    }

    public static PotionEffect createEffect(Player player, PotionEffectType effect) {
        List<Integer> limits = new ArrayList<>();
        if (player != null) player.recalculatePermissions();

        String permPrefix = "potion." + effect.getName().toLowerCase() + ".";

        for (PermissionAttachmentInfo perms : player == null ? Collections.<PermissionAttachmentInfo>emptyList() : player.getEffectivePermissions()) {
            if (!perms.getPermission().startsWith(permPrefix)) continue;

            String[] value = perms.getPermission().split(permPrefix);
            if (value.length > 1 && NumberUtils.isDigits(value[1])) {
                limits.add(Integer.parseInt(value[1]));
            }
        }

        if (limits.isEmpty()) return null;

        return new PotionEffect(effect, Integer.MAX_VALUE, Collections.max(limits), false, false);
    }
}