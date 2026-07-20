package com.olziedev.openirondoorsbyhand;

import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class Openirondoorsbyhand extends SpotPlugin implements Listener {

    private static final String IRON_DOOR_PERMISSION = "evergreen.openirondoors";
    private static final String IRON_TRAPDOOR_PERMISSION = "evergreen.openirontrapdoors";
    private static Openirondoorsbyhand instance = null;

    @Override
    public String getName() {
        return "OpenIronDoorsByHand";
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
    public void onDoorInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        if (type != Material.IRON_DOOR && type != Material.IRON_TRAPDOOR) return;

        Player player = event.getPlayer();

        if (type == Material.IRON_DOOR && !player.hasPermission(IRON_DOOR_PERMISSION)) return;
        if (type == Material.IRON_TRAPDOOR && !player.hasPermission(IRON_TRAPDOOR_PERMISSION)) return;

        boolean mainHandEmpty = player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType().isAir();
        boolean offHandEmpty = player.getInventory().getItemInOffHand() == null || player.getInventory().getItemInOffHand().getType().isAir();

        if (player.isSneaking() && (!mainHandEmpty || !offHandEmpty)) {
            return;
        }

        event.setCancelled(true);

        if (event.getHand() != EquipmentSlot.HAND) return;

        if (type == Material.IRON_DOOR) {
            BlockData data = block.getBlockData();
            if (data instanceof Door) {
                Door door = (Door) data;
                boolean isOpening = !door.isOpen();

                Block topHalf;
                Block bottomHalf;

                if (door.getHalf() == Bisected.Half.TOP) {
                    topHalf = block;
                    bottomHalf = block.getRelative(BlockFace.DOWN);
                } else {
                    topHalf = block.getRelative(BlockFace.UP);
                    bottomHalf = block;
                }

                if (topHalf.getType() == Material.IRON_DOOR && bottomHalf.getType() == Material.IRON_DOOR) {
                    Door topData = (Door) topHalf.getBlockData();
                    Door bottomData = (Door) bottomHalf.getBlockData();

                    topData.setOpen(isOpening);
                    bottomData.setOpen(isOpening);

                    topHalf.setBlockData(topData);
                    bottomHalf.setBlockData(bottomData);

                    Sound sound = isOpening ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE;
                    block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), sound, 1.0f, 1.0f);
                }
            }
        }
        else if (type == Material.IRON_TRAPDOOR) {
            BlockData data = block.getBlockData();
            if (data instanceof TrapDoor) {
                TrapDoor trapdoor = (TrapDoor) data;
                boolean isOpening = !trapdoor.isOpen();

                trapdoor.setOpen(isOpening);
                block.setBlockData(trapdoor);

                Sound sound = isOpening ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
                block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), sound, 1.0f, 1.0f);
            }
        }
    }
}