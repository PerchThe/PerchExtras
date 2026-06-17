package com.olziedev.lapis;

import com.olziedev.spotextras.SpotExtras; // This tells the file where your main class is!

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class LapisListener implements Listener {

    // 1. We changed LapisPersist to SpotExtras here
    private final SpotExtras plugin;
    private final TableManager tableManager;

    // 2. We changed LapisPersist to SpotExtras here as well
    public LapisListener(SpotExtras plugin, TableManager tableManager) {
        this.plugin = plugin;
        this.tableManager = tableManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.ENCHANTING) return;

        EnchantingInventory inv = (EnchantingInventory) event.getInventory();
        Location loc = inv.getLocation();
        if (loc == null) return;

        if (tableManager.isLocked(loc)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cSomeone is already using this enchanting table!");
            return;
        }

        tableManager.lock(loc);

        Block block = loc.getBlock();
        if (block.getState() instanceof TileState tileState) {
            PersistentDataContainer pdc = tileState.getPersistentDataContainer();
            // Because 'plugin' is now SpotExtras, getLapisKey() will perfectly resolve!
            if (pdc.has(plugin.getLapisKey(), PersistentDataType.INTEGER)) {
                Integer storedAmount = pdc.get(plugin.getLapisKey(), PersistentDataType.INTEGER);
                if (storedAmount != null && storedAmount > 0) {
                    inv.setSecondary(new ItemStack(Material.LAPIS_LAZULI, storedAmount));
                    pdc.remove(plugin.getLapisKey());
                    tileState.update();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() != InventoryType.ENCHANTING) return;

        EnchantingInventory inv = (EnchantingInventory) event.getInventory();
        Location loc = inv.getLocation();
        if (loc == null) return;

        ItemStack secondarySlot = inv.getSecondary();
        Block block = loc.getBlock();

        if (block.getState() instanceof TileState tileState) {
            PersistentDataContainer pdc = tileState.getPersistentDataContainer();
            if (secondarySlot != null && secondarySlot.getType() == Material.LAPIS_LAZULI) {
                pdc.set(plugin.getLapisKey(), PersistentDataType.INTEGER, secondarySlot.getAmount());
                tileState.update();
                inv.setSecondary(null);
            } else if (pdc.has(plugin.getLapisKey(), PersistentDataType.INTEGER)) {
                pdc.remove(plugin.getLapisKey());
                tileState.update();
            }
        }

        tableManager.unlock(loc);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.ENCHANTING_TABLE) return;

        tableManager.unlock(block.getLocation());

        if (block.getState() instanceof TileState tileState) {
            PersistentDataContainer pdc = tileState.getPersistentDataContainer();
            if (pdc.has(plugin.getLapisKey(), PersistentDataType.INTEGER)) {
                Integer amount = pdc.get(plugin.getLapisKey(), PersistentDataType.INTEGER);
                if (amount != null && amount > 0) {
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.LAPIS_LAZULI, amount));
                }
            }
        }
    }
}