package com.olziedev.potion.commands;

import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.ExecutorType;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import com.olziedev.potion.Potion;
import com.olziedev.potion.utils.Configuration;
import com.olziedev.potion.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class MilkCommand extends FrameworkCommand {

    public MilkCommand() {
        super("milk");
        this.setPermissions("potion.command.milk");
        this.setExecutorType(ExecutorType.PLAYER_ONLY);
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();

        Bukkit.getScheduler().runTask(Potion.getInstance().getPlugin(), () -> {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
        });

        player.getPersistentDataContainer().remove(Potion.POTION_KEY);

        Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.milk-command"));
    }
}