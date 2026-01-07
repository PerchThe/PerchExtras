package com.olziedev.realestate.utils;

import com.olziedev.olziemenu.framework.menu.FrameworkMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.DecimalFormat;
import java.util.UUID;
import java.util.function.Function;

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

    public static int parseShortTime(String timestr) {
        timestr = timestr.replace("r", "");
        for (String keys : Configuration.getConfig().getConfigurationSection("settings.times").getKeys(false)) {
            for (String s : Configuration.getConfig().getStringList("settings.times." + keys)) {
                timestr = timestr.toLowerCase().replace(s, keys);
            }
        }
        if (!timestr.matches("\\d{1,8}[smhdwM]")) return -1;
        int multiplier = 1;
        switch (timestr.charAt(timestr.length() - 1)) {
            case 'M':
                multiplier *= 4;
            case 'W':    
            case 'w':
                multiplier *= 7;
            case 'D':
            case 'd':
                multiplier *= 24;
            case 'H':    
            case 'h':
                multiplier *= 60;
            case 'm':
                multiplier *= 60;
            case 'S':    
            case 's':
                timestr = timestr.substring(0, timestr.length() - 1);
            default:
        }
        if (Integer.parseInt(timestr) < 0) return -1;

        return (multiplier * Integer.parseInt(timestr));
    }

    public static String formatTime(long seconds) {
        StringBuilder sb = new StringBuilder();
        seconds = addUnit(sb, seconds, 604800, w -> w + "w ");
        seconds = addUnit(sb, seconds, 86400, d -> d + "d ");
        seconds = addUnit(sb, seconds, 3600, h -> h + "h ");
        seconds = addUnit(sb, seconds, 60, m -> m + "m ");
        addUnit(sb, seconds, 1, s -> s + "s");

        String timeString = sb.toString().replaceFirst("(?s)(.*), ", "$1");
        timeString = timeString.replaceFirst("(?s)(.*),", "$1 &");
        return timeString.isEmpty() ? "0s" : timeString;
    }

    private static long addUnit(StringBuilder sb, long sec, long unit, Function<Long, String> s) {
        long n;
        if ((n = sec / unit) > 0) {
            sb.append(s.apply(n));
            sec %= (n * unit);
        }
        return sec;
    }

    public static String formatNumber(double number) {
        return new DecimalFormat(number % 1 == 0 ? "#,###.##" : "#,##0.00").format(number);
    }

    public static void sortInventory(UUID renter) {
        Player player = Bukkit.getPlayer(renter);
        if (player == null) return;

        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (!(inventory.getHolder() instanceof FrameworkMenu)) return;

        player.closeInventory();
    }

    public static String locationString(Location signLocation) {
        int x = (int) signLocation.getX();
        int y = (int) signLocation.getY();
        int z = (int) signLocation.getZ();
        return x + ", " + y + ", " + z;
    }
}
