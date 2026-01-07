package com.olziedev.invisibleitemframes.managers;

import com.olziedev.invisibleitemframes.InvisibleItemFrames;
import com.olziedev.invisibleitemframes.utils.Configuration;
import com.olziedev.invisibleitemframes.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class FrameManager extends Manager {

    private List<EntityType> types;
    private final DatabaseManager manager;

    public FrameManager(InvisibleItemFrames plugin) {
        super(plugin);
        this.manager = InvisibleItemFrames.getDatabaseManager();
    }

    @Override
    public void load() {
    }

    @Override
    public void setup() {
        this.types = Configuration.getConfig().getStringList("settings.types").stream().map(EntityType::valueOf).collect(Collectors.toList());
    }

    public boolean canToggle(Player player, Entity item) {
        return item != null && this.types.contains(item.getType()) && (player == null || manager.getToggled().contains(player.getUniqueId()));
    }

    public boolean openContainer(Player player, Block block) {
        PlayerInteractEvent playerInteractEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(block.getType()), block, BlockFace.SELF);
        Bukkit.getPluginManager().callEvent(playerInteractEvent);
        return !playerInteractEvent.isCancelled();
    }

    public Inventory getInventory(Player player, Block block){
        if (block.getType().equals(Material.ENDER_CHEST)){
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
            return player.getEnderChest();
        }
        if (block.getState() instanceof BlockInventoryHolder) return ((BlockInventoryHolder) block.getState()).getInventory();

        return null;
    }

    public void toggleFrame(Player player, Entity clicked, boolean event) {
        ItemFrame itemFrame = (ItemFrame) clicked;
        if (itemFrame.getItem().getType() == Material.AIR) return;

        boolean isVisible = itemFrame.isVisible();
        if (event) {
            Rotation rotation = itemFrame.getRotation();
            Bukkit.getScheduler().runTaskLater(plugin.plugin, () -> {
                itemFrame.setRotation(rotation);
            }, 1L);
        }
        if (isVisible) {
            Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.hide-frame"));
            itemFrame.setVisible(false);
            return;
        }
        Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.show-frame"));
        itemFrame.setVisible(true);
    }

    public void toggleGlow(Player player, Entity clicked) {
        ItemFrame itemFrame = (ItemFrame) clicked;

        boolean isGlowing = itemFrame.isGlowing();
        if (isGlowing) {
            Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.hide-glow"));
            itemFrame.setGlowing(false);
            return;
        }
        Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.show-glow"));
        itemFrame.setGlowing(true);
    }
}
