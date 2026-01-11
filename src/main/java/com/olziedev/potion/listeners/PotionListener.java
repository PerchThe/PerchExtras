package com.olziedev.potion.listeners;

import com.olziedev.potion.Potion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

public class PotionListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        restorePotions(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Bukkit.getScheduler().runTaskLater(Potion.getInstance().getPlugin(), () -> {
            restorePotions(event.getPlayer());
        }, 1L);
    }

    private void restorePotions(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        if (!pdc.has(Potion.POTION_KEY, PersistentDataType.STRING)) return;

        String data = pdc.get(Potion.POTION_KEY, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) return;

        Arrays.stream(data.split(","))
                .forEach(name -> {
                    PotionEffectType type = PotionEffectType.getByName(name);
                    if (type != null) {
                        PotionEffect effect = Potion.createEffect(player, type);
                        if (effect != null) {
                            player.addPotionEffect(effect);
                        }
                    }
                });
    }
}