package com.olziedev.potion.commands;

import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.ExecutorType;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import com.olziedev.potion.Potion;
import com.olziedev.potion.managers.DatabaseManager;
import com.olziedev.potion.player.PotionPlayer;
import com.olziedev.potion.utils.Configuration;
import com.olziedev.potion.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class MilkCommand extends FrameworkCommand {

    private final DatabaseManager manager = Potion.getDatabaseManager();

    public MilkCommand() {
        super("milk");
        this.setPermissions("potion.command.milk");
        this.setExecutorType(ExecutorType.PLAYER_ONLY);
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();
        PotionPlayer potionPlayer = manager.getPlayer(player.getUniqueId());
        List<PotionEffectType> effects = potionPlayer.getPotions();
        Bukkit.getScheduler().runTask(plugin, () -> effects.forEach(player::removePotionEffect));
        potionPlayer.setPotions(new ArrayList<>());
        Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.milk-command"));
    }
}
