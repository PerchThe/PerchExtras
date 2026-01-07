package com.olziedev.invisibleitemframes.events;

import com.olziedev.invisibleitemframes.InvisibleItemFrames;
import com.olziedev.invisibleitemframes.managers.FrameManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.EnderChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InteractEvent implements Listener {

    private final FrameManager frameManager = InvisibleItemFrames.getFrameManager();
    public static List<UUID> players = new ArrayList<>();
    public static NamespacedKey glowKey = new NamespacedKey(InvisibleItemFrames.getInstance().plugin, "glow_itemframe");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void hideFrame(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        Player player = event.getPlayer();
        boolean contains = players.remove(player.getUniqueId());
        PersistentDataContainer container = player.getPersistentDataContainer();
        if (container.has(glowKey, PersistentDataType.BYTE)) {
            container.remove(glowKey);
            if (event.isCancelled()) return;

            frameManager.toggleGlow(player, clicked);
            return;
        }
        if (event.isCancelled()) return;

        if (contains) {
            frameManager.toggleFrame(player, clicked, false);
            return;
        }
        if (!event.getHand().equals(EquipmentSlot.HAND) || !frameManager.canToggle(player, clicked) || !player.isSneaking()) return;

        frameManager.toggleFrame(player, clicked, true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Entity entity = player.getTargetEntity(5);
        if (entity == null) {
            return;
        }
        boolean contains = players.remove(player.getUniqueId());
        PersistentDataContainer container = player.getPersistentDataContainer();
        if (container.has(glowKey, PersistentDataType.BYTE)) {
            container.remove(glowKey);
            if (event.useInteractedBlock() == Event.Result.DENY) return;

            frameManager.toggleGlow(player, entity);
            return;
        }
        if (event.useInteractedBlock() == Event.Result.DENY || !contains) {
            return;
        }
        frameManager.toggleFrame(player, entity, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void useFrame(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof ItemFrame)) return;

        ItemFrame itemFrame = (ItemFrame) clicked;
        boolean contains = players.remove(player.getUniqueId());
        PersistentDataContainer container = player.getPersistentDataContainer();
        if (container.has(glowKey, PersistentDataType.BYTE)) {
            container.remove(glowKey);
            if (event.isCancelled()) return;

            frameManager.toggleGlow(player, itemFrame);
            return;
        }
        if (event.isCancelled()) return;

        if (contains) {
            frameManager.toggleFrame(player, itemFrame, false);
            return;
        }
        if (!event.getHand().equals(EquipmentSlot.HAND) || !frameManager.canToggle(null, clicked) || itemFrame.isVisible()) return;

        Block block = clicked.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
        if (block == null) return;

        event.setCancelled(true);
        if (player.isSneaking()) return;

        BlockState state = block.getState();
        if ((!(state instanceof Container) && !(state instanceof EnderChest))) return;
        if (!frameManager.openContainer(player, block)) return;

        Inventory i = frameManager.getInventory(player, block);
        if (i == null) return;

        player.openInventory(i);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void damageFrame(EntityDamageByEntityEvent event) {
        Entity clicked = event.getEntity();
        if (event.isCancelled() || !frameManager.canToggle(null, clicked)) return;

        ItemFrame itemFrame = (ItemFrame) clicked;
        itemFrame.setVisible(true);
    }
}
