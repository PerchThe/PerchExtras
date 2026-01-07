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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionCommand extends FrameworkCommand {

    private final DatabaseManager manager = Potion.getDatabaseManager();

    private final PotionEffectType potionType;

    public PotionCommand(PotionEffectType potionType, String name) {
        super(name);
        this.potionType = potionType;
        this.setExecutorType(ExecutorType.PLAYER_ONLY);
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();
        PotionPlayer potionPlayer = manager.getPlayer(player.getUniqueId());
        if (player.hasPotionEffect(potionType)) {
            Bukkit.getScheduler().runTask(plugin, () -> player.removePotionEffect(potionType));
            potionPlayer.remove(potionType);
            Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.disabled").replace("%potion%", this.getPotionName()));
            return;
        }
        PotionEffect effect = potionPlayer.createEffect(potionType);
        if (effect == null) {
            Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.no-limit-permission"));
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> player.addPotionEffect(effect));
        potionPlayer.add(potionType);
        Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.enabled").replace("%potion%", this.getPotionName()));
    }

    public String getPotionName() {
        return Configuration.getString(Configuration.getConfig(), "settings.potions." + potionType.getName() + ".name");
    }
}
