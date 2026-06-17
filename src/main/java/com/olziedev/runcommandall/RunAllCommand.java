package com.olziedev.runcommandall;

import com.olziedev.spotextras.SpotExtras;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RunAllCommand implements CommandExecutor {

    private final SpotExtras plugin;

    public RunAllCommand(SpotExtras plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Updated the permission node to reflect the new Extras naming
        if (!sender.hasPermission("perchextras.runcommandall")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /runcommandall <command with $player>");
            return true;
        }

        // Join all arguments to form the command template
        String baseCommand = String.join(" ", args);

        for (Player player : Bukkit.getOnlinePlayers()) {
            String cmd = baseCommand.replace("$player", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        sender.sendMessage("§aCommand executed for all online players.");
        return true;
    }
}