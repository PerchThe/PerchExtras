package com.olziedev.antibackteleport.commands;

import com.earth2me.essentials.User;
import com.olziedev.antibackteleport.AntiBackTeleport;
import com.olziedev.antibackteleport.addon.EssentialsAddon;
import com.olziedev.antibackteleport.manager.AddonManager;
import com.olziedev.antibackteleport.utils.Configuration;
import com.olziedev.antibackteleport.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class AntiBackTeleportCommand extends FrameworkCommand {

    private final AddonManager addonManager = AntiBackTeleport.getAddonManager();

    public AntiBackTeleportCommand() {
        super("teleportwithoutback");
        this.setPermissions("antibackteleport.command");
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        String[] args = cmd.getArguments();
        if (args.length < 5) {
            Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.command-format").replace("%cmd%", cmd.getLabel()));
            return;
        }
        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.no-player-joined"));
            return;
        }
        User user = addonManager.getAddon(EssentialsAddon.class).getUser(player.getUniqueId());
        if (user == null) {
            Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.no-player-joined"));
            return;
        }
        try {
            Location lastLocation = user.getLastLocation();

            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.invalid-world"));
                return;
            }
            Location location = new Location(world, Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));

            if (args.length == 6) location.setYaw(Float.parseFloat(args[5]));
            if (args.length == 7) location.setPitch(Float.parseFloat(args[6]));

            Bukkit.getScheduler().runTask(AntiBackTeleport.getInstance().plugin, () -> {
                player.teleportAsync(location).thenAccept(success -> {
                    user.setLastLocation(lastLocation);
                    Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.teleport").replace("%player%", player.getName()));
                });
            });
        } catch (Throwable ex) {
            Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.invalid-location-format"));
        }
    }
}
