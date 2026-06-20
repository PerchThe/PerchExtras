package com.olziedev.lapis;

import com.olziedev.spotextras.SpotExtras;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LapisListener implements Listener {

    private final SpotExtras plugin;
    private final Set<Location> inUseTables = new HashSet<>();

    public LapisListener(SpotExtras plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.ENCHANTING) return;

        EnchantingInventory inv = (EnchantingInventory) event.getInventory();
        if (inv.getLocation() == null) return;

        Location loc = inv.getLocation().getBlock().getLocation();

        if (inUseTables.contains(loc)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cSomeone is already using this enchanting table!");
            return;
        }

        inUseTables.add(loc);

        Block block = loc.getBlock();
        if (block.getState() instanceof TileState tileState) {
            PersistentDataContainer pdc = tileState.getPersistentDataContainer();

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
        if (inv.getLocation() == null) return;

        Location loc = inv.getLocation().getBlock().getLocation();

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

        inUseTables.remove(loc);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleTableDestruction(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosionList(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosionList(event.blockList());
    }

    private void handleExplosionList(List<Block> blocks) {
        for (Block block : blocks) {
            handleTableDestruction(block);
        }
    }

    private void handleTableDestruction(Block block) {
        if (block.getType() != Material.ENCHANTING_TABLE) return;

        inUseTables.remove(block.getLocation());

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

    public void clearAllLocks() {
        inUseTables.clear();
    }
}