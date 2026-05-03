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
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class BreakEvent implements Listener {

    private final GriefPreventionAddon griefPrevention;

    // Faces where a sign could potentially be attached
    private static final BlockFace[] SIGN_FACES = {BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    public BreakEvent() {
        this.griefPrevention = RealEstate.getAddonManager().getAddon(GriefPreventionAddon.class);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // 1. Check if the block being broken IS an estate
        if (this.isBreakable(event, block)) return;

        // 2. Check if the block being broken is SUPPORTING an estate
        for (BlockFace face : SIGN_FACES) {
            Block attachedBlock = block.getRelative(face);
            if (isEstate(attachedBlock) && isAttachedTo(attachedBlock, block)) {
                Player player = event.getPlayer();
                Utils.sendMessage(player, Configuration.getConfig().getString("lang.cannot-break", "&cYou cannot break the block supporting this estate!"));
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Remove the block from the explosion if it is an estate OR supports an estate
        event.blockList().removeIf(this::isEstateOrSupporting);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isEstateOrSupporting);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isEstateOrSupporting)) {
            event.setCancelled(true);
        }
    }

    // Added to prevent sticky pistons from pulling the block out from under a sign
    @EventHandler(priority = EventPriority.HIGH)
    public void onRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isEstateOrSupporting)) {
            event.setCancelled(true);
        }
    }

    public boolean isEstate(Block block) {
        Location location = block.getLocation();
        Claim claim = griefPrevention.getClaim(location);
        if (claim == null) return false;

        return RealEstate.getDatabaseManager().getEState(block.getLocation(), EState.class) != null;
    }

    // Helper method to check if a block is an estate OR is holding an estate up
    public boolean isEstateOrSupporting(Block block) {
        if (isEstate(block)) return true;

        for (BlockFace face : SIGN_FACES) {
            Block attachedBlock = block.getRelative(face);
            if (isEstate(attachedBlock) && isAttachedTo(attachedBlock, block)) {
                return true;
            }
        }
        return false;
    }

    // Safely checks if the sign block is physically attached to the supporting block
    private boolean isAttachedTo(Block signBlock, Block supportingBlock) {
        try {
            // Modern Bukkit (1.13+)
            BlockData blockData = signBlock.getBlockData();
            if (blockData instanceof org.bukkit.block.data.type.WallSign) {
                org.bukkit.block.data.type.WallSign sign = (org.bukkit.block.data.type.WallSign) blockData;
                return signBlock.getRelative(sign.getFacing().getOppositeFace()).equals(supportingBlock);
            } else if (blockData instanceof org.bukkit.block.data.type.Sign) {
                return signBlock.getRelative(BlockFace.DOWN).equals(supportingBlock);
            }
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            // Legacy Bukkit Support (1.8 - 1.12.2)
            org.bukkit.material.MaterialData data = signBlock.getState().getData();
            if (data instanceof org.bukkit.material.Sign) {
                org.bukkit.material.Sign sign = (org.bukkit.material.Sign) data;
                return signBlock.getRelative(sign.getAttachedFace()).equals(supportingBlock);
            }
        }
        return false;
    }

    public boolean isBreakable(BlockBreakEvent event, Block block) {
        Location location = block.getLocation();
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
        block.breakNaturally();
        return true;
    }
}