package com.olziedev.potion.events;

import com.olziedev.potion.Potion;
import com.olziedev.potion.managers.DatabaseManager;
import com.olziedev.potion.player.PotionPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class JoinEvent implements Listener {

    private final DatabaseManager manager = Potion.getDatabaseManager();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PotionPlayer potionPlayer = manager.getPlayer(player.getUniqueId());
        potionPlayer.getPotions().forEach(type -> {
            PotionEffectType potionType = manager.getPotions().get(type.getName());
            player.removePotionEffect(type);
            PotionEffect effect = potionPlayer.createEffect(potionType);
            if (effect == null) return;

            player.addPotionEffect(effect);
        });
    }
}
