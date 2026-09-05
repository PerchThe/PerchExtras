package com.olziedev.preventplayersfromdestroyingtheirseeb;

import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

public class Preventplayersfromdestroyingtheirseeb extends SpotPlugin implements Listener {

    private static final NamespacedKey STARTER_ITEM_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("evergreen:starteritem")
    );

    @Override
    public String getName() {
        return "preventplayersfromdestroyingtheirseeb";
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    @Override
    public void onDisable() {
        // This module has no state to clean up.
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();

        if (meta != null && meta.getPersistentDataContainer().has(STARTER_ITEM_KEY)) {
            event.setCancelled(true);
        }
    }
}
