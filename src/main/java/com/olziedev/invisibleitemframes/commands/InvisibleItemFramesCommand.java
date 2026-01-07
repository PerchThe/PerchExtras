package com.olziedev.invisibleitemframes.commands;

import com.olziedev.invisibleitemframes.InvisibleItemFrames;
import com.olziedev.invisibleitemframes.events.InteractEvent;
import com.olziedev.invisibleitemframes.managers.FrameManager;
import com.olziedev.invisibleitemframes.utils.Configuration;
import com.olziedev.invisibleitemframes.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.ExecutorType;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InvisibleItemFramesCommand extends FrameworkCommand {

    private final FrameManager manager = InvisibleItemFrames.getFrameManager();

    public InvisibleItemFramesCommand() {
        super("invisibleitemframes");
        this.setAliases("iif");
        this.setPermissions("invisibleitemframes.use");
        this.setExecutorType(ExecutorType.PLAYER_ONLY);
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();
        // check if the current block the player is looking at is an item frame
        Entity entity = player.getTargetEntity(5);
        if (!manager.canToggle(null, entity) || InteractEvent.players.contains(player.getUniqueId())) {
            Utils.sendMessage(player, Configuration.getConfig().getString("lang.not-itemframe"));
            return;
        }
        InteractEvent.players.add(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, entity);
            Bukkit.getPluginManager().callEvent(event);
        });
    }
}
