package com.olziedev.antibackteleport.commands;

import com.olziedev.antibackteleport.AntiBackTeleport;
import com.olziedev.antibackteleport.addon.WorldguardAddon;
import com.olziedev.antibackteleport.manager.AddonManager;
import com.olziedev.antibackteleport.utils.Configuration;
import com.olziedev.antibackteleport.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class RegionPlayersCommand extends FrameworkCommand {

    private final AddonManager addonManager = AntiBackTeleport.getAddonManager();

    public RegionPlayersCommand() {
        super("executeconsolecommandonplayersinsideregion");
        this.setDescription("Execute a console command on players inside a region");
        this.setPermissions("antibackteleport.regionplayers.command");
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        String[] args = cmd.getArguments();
        if (args.length < 3) {
            Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.regionplayers-format"));
            return;
        }
        World world = Bukkit.getWorld(args[0]);
        if (world == null) {
            Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.world-not-found"));
            return;
        }
        String regionName = args[1];
        String command = String.join(" ", args).substring(world.getName().length() + regionName.length() + 2);
        addonManager.getAddon(WorldguardAddon.class).getPlayersInRegion(world, regionName, players -> {
            if (players == null) {
                Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.region-error"));
                return;
            }
            players.forEach(player -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("!player", player.getName()));
            });
        });
    }
}
