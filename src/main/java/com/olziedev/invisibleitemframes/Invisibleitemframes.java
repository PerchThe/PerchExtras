package com.olziedev.invisibleitemframes;

import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;

public class Invisibleitemframes extends SpotPlugin implements Listener {

    private static Invisibleitemframes instance = null;

    @Override
    public String getName() {
        return "InvisibleItemFrames";
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
    public void onFrameInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame)) return;

        ItemFrame frame = (ItemFrame) event.getRightClicked();
        boolean isSneaking = event.getPlayer().isSneaking();
        boolean hasItem = frame.getItem().getType() != Material.AIR;
        boolean isVisible = frame.isVisible();

        if (!isVisible) {
            event.setCancelled(true);
            if (event.getHand() == EquipmentSlot.HAND) {
                if (isSneaking) {
                    frame.setVisible(true);
                } else {
                    openContainerBehind(frame, event.getPlayer());
                }
            }
            return;
        }

        if (isVisible && isSneaking && hasItem) {
            event.setCancelled(true);

            if (event.getHand() == EquipmentSlot.HAND) {
                frame.setVisible(false);
            }
        }
    }

    private void openContainerBehind(ItemFrame frame, org.bukkit.entity.Player player) {
        Block attachedBlock = frame.getLocation().getBlock().getRelative(frame.getAttachedFace());

        if (!attachedBlock.getType().isInteractable()) return;

        PlayerInteractEvent dummyInteract = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItemInMainHand(),
                attachedBlock,
                frame.getAttachedFace(),
                EquipmentSlot.HAND
        );
        Bukkit.getPluginManager().callEvent(dummyInteract);

        if (dummyInteract.isCancelled() || dummyInteract.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        if (attachedBlock.getType() == Material.ENDER_CHEST) {
            player.openInventory(player.getEnderChest());
            player.playSound(attachedBlock.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
        }

        else if (attachedBlock.getState() instanceof InventoryHolder) {
            player.openInventory(((InventoryHolder) attachedBlock.getState()).getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame)) return;

        if (!((ItemFrame) event.getEntity()).isVisible()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame)) return;

        if (!((ItemFrame) event.getEntity()).isVisible()) {
            if (event.getCause() != HangingBreakEvent.RemoveCause.PHYSICS) {
                event.setCancelled(true);
            }
        }
    }
}