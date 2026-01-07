package com.olziedev.realestate.addons;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

public class GriefPreventionAddon extends Addon {

    public GriefPrevention instance;

    public GriefPreventionAddon(RealEstate plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        instance = GriefPrevention.instance;
    }

    public Claim getClaim(long id) {
        return instance.dataStore.getClaim(id);
    }

    public Claim getClaim(Location location) {
        return instance.dataStore.getClaimAt(location, true, null);
    }

    public boolean isPart(Claim claim, UUID uuid, boolean includeTeam) {
        if (claim == null) return false;

        return (includeTeam && claim.hasExplicitPermission(uuid, ClaimPermission.Access)) || claim.managers.contains(uuid.toString()) || claim.ownerID.equals(uuid);
    }

    @EventHandler
    public void disableDeleteCommand(PlayerCommandPreprocessEvent event) {
        PluginCommand pluginCommand = Bukkit.getServer().getPluginCommand(event.getMessage().split(" ")[0].substring(1));
        Player player = event.getPlayer();
        if (pluginCommand == null || !pluginCommand.getPlugin().getName().equals("GriefPrevention") || player.hasPermission("realestate.deleteclaims.bypass")) return;

        boolean eState = RealEstate.getDatabaseManager().getEStates().values().stream().anyMatch(x -> x.getOwner().equals(player.getUniqueId()));
        if (!eState) return;

        switch (pluginCommand.getName()) {
            case "abandonallclaims":
                Utils.sendMessage(player, Configuration.getConfig().getString("lang.disabled-command"));
                event.setCancelled(true);
            case "extendclaim":
            case "expandclaim":
            case "resizeclaim":
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onModifyClaim(PlayerCommandPreprocessEvent event) {
        PluginCommand pluginCommand = Bukkit.getServer().getPluginCommand(event.getMessage().split(" ")[0].substring(1));
        Player player = event.getPlayer();
        if (pluginCommand == null || !pluginCommand.getPlugin().getName().equals("GriefPrevention")) return;

        Claim claim = this.getClaim(player.getLocation());
        if (claim == null) return;

        EState eState = RealEstate.getDatabaseManager().getEState(claim.getID(), EState.class);
        if (eState == null) return;

        switch (pluginCommand.getName()) {
            case "abandonclaim":
            case "untrust":
            case "deleteclaim":
            case "extendclaim":
                Utils.sendMessage(player, Configuration.getConfig().getString("lang.modify-claim"));
                event.setCancelled(true);
        }
    }
}
