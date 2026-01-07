package com.olziedev.realestate.events;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.managers.DatabaseManager;
import com.olziedev.realestate.player.EStatePlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;

public class JoinEvent implements Listener {

    private final DatabaseManager manager;

    public JoinEvent() {
        manager = RealEstate.getDatabaseManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        EStatePlayer eStatePlayer = manager.getPlayer(event.getPlayer().getUniqueId());

        for (String message : new ArrayList<>(eStatePlayer.getMessages())) {
            Utils.sendMessage(player, message);
            eStatePlayer.manageMessage(message, false);
        }
        for (String message : new ArrayList<>(eStatePlayer.getDismissMessages())) {
            Utils.sendMessage(player, message);
        }
        for (Long reminders : eStatePlayer.getReminders()) {
            RentingEstate rentingEstate = manager.getEState(reminders, RentingEstate.class);
            if (rentingEstate == null) continue;

            Utils.sendMessage(player, Configuration.getConfig().getString("lang.reminder").replace("%player%", Bukkit.getOfflinePlayer(rentingEstate.getOwner()).getName()).replace("%location%", Utils.locationString(rentingEstate.getSignLocation())).replace("%price%", Utils.formatNumber(rentingEstate.getPrice())));
        }
    }
}
