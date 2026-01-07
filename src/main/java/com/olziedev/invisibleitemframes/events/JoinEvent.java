package com.olziedev.invisibleitemframes.events;

import com.olziedev.invisibleitemframes.InvisibleItemFrames;
import com.olziedev.invisibleitemframes.managers.DatabaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinEvent implements Listener {
    
    private final DatabaseManager manager = InvisibleItemFrames.getDatabaseManager();

    @EventHandler
    public void onJoin(AsyncPlayerPreLoginEvent event) {
        manager.loadToggled(event.getUniqueId());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        manager.getToggled().remove(event.getPlayer().getUniqueId());
    }
}
