package com.olziedev.invisibleitemframes.commands.subcommands;

import com.olziedev.invisibleitemframes.InvisibleItemFrames;
import com.olziedev.invisibleitemframes.events.InteractEvent;
import com.olziedev.invisibleitemframes.managers.FrameManager;
import com.olziedev.invisibleitemframes.utils.Configuration;
import com.olziedev.invisibleitemframes.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

public class GlowCommand extends FrameworkSubCommand {

    private final FrameManager manager = InvisibleItemFrames.getFrameManager();

    public GlowCommand() {
        super("glow");
        this.setParent("invisibleitemframes");
        this.setPermissions("invisibleitemframes.glow");
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();
        // check if the current block the player is looking at is an item frame
        Entity entity = player.getTargetEntity(5);
        if (!manager.canToggle(null, entity)) {
            Utils.sendMessage(player, Configuration.getConfig().getString("lang.not-itemframe"));
            return;
        }
        player.getPersistentDataContainer().set(InteractEvent.glowKey, PersistentDataType.BYTE, (byte) 0);
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, entity);
            Bukkit.getPluginManager().callEvent(event);
        });
    }
}
