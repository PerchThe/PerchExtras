package com.olziedev.potion.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Utils {

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void sendMessage(CommandSender sender, String s) {
        if (s == null || s.trim().isEmpty() || sender == null) return;

        s = color(s);
        if (!(sender instanceof Player)) {
            Bukkit.getServer().getConsoleSender().sendMessage(s);
            return;
        }
        sender.sendMessage(s);
    }
}
