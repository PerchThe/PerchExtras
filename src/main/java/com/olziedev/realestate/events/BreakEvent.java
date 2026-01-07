package com.olziedev.realestate.events;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.GriefPreventionAddon;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Arrays;

public class BreakEvent implements Listener {

    private final GriefPreventionAddon griefPrevention;

    public BreakEvent() {
        this.griefPrevention = RealEstate.getAddonManager().getAddon(GriefPreventionAddon.class);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        for (int i : Arrays.asList(0, 1)) {
            if (this.isBreakable(event, location.clone().add(0, i, 0))) return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isEstate);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isEstate);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isEstate)) {
            event.setCancelled(true);
        }
    }

    public boolean isEstate(Block block) {
        Location location = block.getLocation();
        Claim claim = griefPrevention.getClaim(location);
        if (claim == null) return false;

        return RealEstate.getDatabaseManager().getEState(block.getLocation(), EState.class) != null;
    }

    public boolean isBreakable(BlockBreakEvent event, Location location) {
        Claim claim = griefPrevention.getClaim(location);
        if (claim == null) return false;

        EState eState = RealEstate.getDatabaseManager().getEState(location, EState.class);
        if (eState == null) return false;

        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(eState.getOwner()) && !player.hasPermission("realestate.admin")) {
            Utils.sendMessage(player, Configuration.getConfig().getString("lang.cannot-break").replace("%owner%", eState.getOwnerName()));
            event.setCancelled(true);
            return true;
        }

        if (eState instanceof RentingEstate) {
            RentingEstate rentingEstate = (RentingEstate) eState;
            if (rentingEstate.getRenter() != null) {
                Utils.sendMessage(player, Configuration.getConfig().getString("lang.cannot-break-rent").replace("%player%", rentingEstate.getRenterName()));
                event.setCancelled(true);
                return true;
            }
        }
        eState.remove();
        Utils.sendMessage(player, Configuration.getConfig().getString("lang.broke-estate"));
        event.setCancelled(true);
        event.getBlock().breakNaturally();
        return true;
    }
}
