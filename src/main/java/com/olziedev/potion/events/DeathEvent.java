package com.olziedev.potion.events;

import com.olziedev.potion.Potion;
import com.olziedev.potion.managers.DatabaseManager;
import com.olziedev.potion.player.PotionPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DeathEvent implements Listener {

    private final DatabaseManager manager = Potion.getDatabaseManager();

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PotionPlayer potionPlayer = manager.getPlayer(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(manager.plugin.plugin, () -> {
            potionPlayer.getPotions().forEach(type -> {
                PotionEffectType potionType = manager.getPotions().get(type.getName());
                player.removePotionEffect(type);

                PotionEffect effect = potionPlayer.createEffect(potionType);
                if (effect == null) return;

                player.addPotionEffect(effect);
            });
        }, 5L);
    }
}
