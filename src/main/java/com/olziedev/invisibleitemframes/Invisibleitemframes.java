package com.olziedev.invisibleitemframes;

import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
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
import org.bukkit.block.Container;

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

    // Changed to LOW and removed ignoreCancelled so we catch the click
    // BEFORE claim plugins mistakenly block the entity interaction.
    @EventHandler(priority = EventPriority.LOW)
    public void onFrameInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame)) return;

        ItemFrame frame = (ItemFrame) event.getRightClicked();
        boolean isSneaking = event.getPlayer().isSneaking();
        // .isAir() is safer for modern versions than checking != Material.AIR
        boolean hasItem = !frame.getItem().getType().isAir();
        boolean isVisible = frame.isVisible();

        // SCENARIO 1: The frame is INVISIBLE
        if (!isVisible) {
            event.setCancelled(true); // Stop vanilla rotation/claim plugin interference

            if (event.getHand() == EquipmentSlot.HAND) {
                if (isSneaking) {
                    frame.setVisible(true); // Un-invisible it
                } else {
                    openContainerBehind(frame, event.getPlayer()); // Open the container
                }
            }
            return;
        }

        // SCENARIO 2: The frame is VISIBLE
        if (isVisible && isSneaking) {
            if (hasItem) {
                // If it HAS an item, make it invisible
                event.setCancelled(true);
                if (event.getHand() == EquipmentSlot.HAND) {
                    frame.setVisible(false);
                }
            }
            // If it DOES NOT have an item, it just skips this block entirely.
            // Vanilla behavior takes over, which perfectly prevents them from
            // making an empty frame invisible without throwing any ugly errors!
        }
    }

    private void openContainerBehind(ItemFrame frame, Player player) {
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
        else if (attachedBlock.getState() instanceof Container) {
            player.openInventory(((Container) attachedBlock.getState()).getInventory());

            // Just a little polish: Added the barrel open sound to match your Ender Chest logic!
            if (attachedBlock.getType().name().contains("BARREL")) {
                player.playSound(attachedBlock.getLocation(), Sound.BLOCK_BARREL_OPEN, 1.0f, 1.0f);
            }
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