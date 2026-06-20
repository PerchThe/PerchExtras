package com.olziedev.openblockedcontainers;

import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;

public class Openblockedcontainers extends SpotPlugin implements Listener {

    private static Openblockedcontainers instance = null;

    @Override
    public String getName() {
        return "OpenBlockedContainers";
    }

    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();

        boolean mainHandEmpty = player.getInventory().getItemInMainHand().getType().isAir();
        boolean offHandEmpty = player.getInventory().getItemInOffHand().getType().isAir();

        if (player.isSneaking() && (!mainHandEmpty || !offHandEmpty)) {
            return;
        }

        Material type = block.getType();
        boolean isShulker = type.name().endsWith("SHULKER_BOX");
        boolean isChest = type == Material.CHEST || type == Material.TRAPPED_CHEST;
        boolean isEnderChest = type == Material.ENDER_CHEST;

        if (!isChest && !isShulker && !isEnderChest) return;

        if (!isOccluded(block, type, isShulker)) return;

        event.setCancelled(true);

        if (event.getHand() == EquipmentSlot.HAND) {
            if (isEnderChest) {
                player.openInventory(player.getEnderChest());
                player.playSound(block.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
            } else if (block.getState() instanceof InventoryHolder) {
                InventoryHolder holder = (InventoryHolder) block.getState();
                player.openInventory(holder.getInventory());
            }
        }
    }

    private boolean isOccluded(Block block, Material type, boolean isShulker) {
        if (isShulker) {
            if (block.getBlockData() instanceof Directional) {
                Directional dir = (Directional) block.getBlockData();
                return block.getRelative(dir.getFacing()).getType().isSolid();
            }
            return false;
        }

        if (type == Material.ENDER_CHEST) {
            return block.getRelative(BlockFace.UP).getType().isSolid();
        }

        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();

            if (chest.getInventory().getHolder() instanceof DoubleChest) {
                DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
                InventoryHolder left = doubleChest.getLeftSide();
                InventoryHolder right = doubleChest.getRightSide();

                if (left instanceof Chest && ((Chest) left).getBlock().getRelative(BlockFace.UP).getType().isSolid()) return true;
                if (right instanceof Chest && ((Chest) right).getBlock().getRelative(BlockFace.UP).getType().isSolid()) return true;
                return false;
            } else {
                return block.getRelative(BlockFace.UP).getType().isSolid();
            }
        }
        return false;
    }
}