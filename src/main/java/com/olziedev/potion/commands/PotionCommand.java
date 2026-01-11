package com.olziedev.potion.commands;

import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.ExecutorType;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import com.olziedev.potion.Potion;
import com.olziedev.potion.utils.Configuration;
import com.olziedev.potion.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PotionCommand extends FrameworkCommand {

    private final PotionEffectType potionType;

    public PotionCommand(PotionEffectType potionType, String name) {
        super(name);
        this.potionType = potionType;
        this.setExecutorType(ExecutorType.PLAYER_ONLY);
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        List<String> savedPotions = new ArrayList<>();
        if (pdc.has(Potion.POTION_KEY, PersistentDataType.STRING)) {
            String stored = pdc.get(Potion.POTION_KEY, PersistentDataType.STRING);
            if (stored != null && !stored.isEmpty()) {
                savedPotions.addAll(Arrays.asList(stored.split(",")));
            }
        }

        if (player.hasPotionEffect(potionType) || savedPotions.contains(potionType.getName())) {

            Bukkit.getScheduler().runTask(Potion.getInstance().getPlugin(), () -> player.removePotionEffect(potionType));
            savedPotions.remove(potionType.getName());
            updatePDC(player, savedPotions);

            Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.disabled")
                    .replace("%potion%", this.getPotionName()));
            return;
        }

        PotionEffect effect = Potion.createEffect(player, potionType);

        if (effect == null) {
            Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.no-limit-permission"));
            return;
        }

        Bukkit.getScheduler().runTask(Potion.getInstance().getPlugin(), () -> player.addPotionEffect(effect));

        if (!savedPotions.contains(potionType.getName())) {
            savedPotions.add(potionType.getName());
        }

        updatePDC(player, savedPotions);

        Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.enabled")
                .replace("%potion%", this.getPotionName()));
    }

    private void updatePDC(Player player, List<String> potions) {
        if (potions.isEmpty()) {
            player.getPersistentDataContainer().remove(Potion.POTION_KEY);
        } else {
            String data = String.join(",", potions);
            player.getPersistentDataContainer().set(Potion.POTION_KEY, PersistentDataType.STRING, data);
        }
    }

    public String getPotionName() {
        return Configuration.getString(Configuration.getConfig(), "settings.potions." + potionType.getName() + ".name");
    }
}