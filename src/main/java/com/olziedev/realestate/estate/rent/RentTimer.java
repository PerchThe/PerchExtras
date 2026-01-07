package com.olziedev.realestate.estate.rent;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.VaultAddon;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.managers.DatabaseManager;
import com.olziedev.realestate.player.EStatePlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Date;
import java.util.List;

public class RentTimer extends BukkitRunnable {

    private final DatabaseManager manager = RealEstate.getDatabaseManager();

    @Override
    @SuppressWarnings("all")
    public void run() {
        VaultAddon vaultAddon = RealEstate.getAddonManager().getAddon(VaultAddon.class);
        for (EState states : manager.getEStates().values()) {
            if (!(states instanceof RentingEstate)) continue;

            RentingEstate estate = (RentingEstate) states;
            if (estate.getRenter() == null || estate.getActivatedFlags().contains(RentFlags.NICEMODE)) continue;

            long paidPrice = estate.getPaidPrice();
            if (new Date().getTime() >= paidPrice) {
                this.handleNextTime(vaultAddon, estate);

                if (estate.getActivatedFlags().contains(RentFlags.RENEW) && vaultAddon.economy.has(Bukkit.getOfflinePlayer(estate.getRenter()), estate.getPrice()) && !estate.isCancelled()) {
                    manager.plugin.getLogger().info("The rent has been renewed.");
                    estate.setNextTime();
                    estate.setPaidPrice(-1, estate.getRenter());
                    manager.getPlayer(estate.getOwner()).manageDismissMessage(Configuration.getConfig().getString("lang.rent-renewed")
                            .replace("%location%", Utils.locationString(estate.getSignLocation()))
                            .replace("%player%", Bukkit.getOfflinePlayer(estate.getRenter()).getName()), true);
                    manager.getPlayer(estate.getRenter()).manageDismissMessage(Configuration.getConfig().getString("lang.rent-renewed-other")
                            .replace("%location%", Utils.locationString(estate.getSignLocation()))
                            .replace("%player%", Bukkit.getOfflinePlayer(estate.getOwner()).getName()), true);

                    Bukkit.getScheduler().runTask(RealEstate.getInstance().plugin, () -> {
                        Utils.sortInventory(estate.getRenter());
                        Utils.sortInventory(estate.getOwner());
                    });
                    continue;
                }
                if (estate.getRentFlags().contains(RentFlags.NICEMODE)) {
                    manager.plugin.getLogger().info("The rent has been ended, setting it to nice mode.");
                    estate.setHold();
                    manager.getPlayer(estate.getOwner()).manageDismissMessage(Configuration.getConfig().getString("lang.rent-hold")
                            .replace("%location%", Utils.locationString(estate.getSignLocation()))
                            .replace("%player%", Bukkit.getOfflinePlayer(estate.getRenter()).getName()), true);
                    manager.getPlayer(estate.getRenter()).manageDismissMessage(Configuration.getConfig().getString("lang.rent-hold-other")
                            .replace("%location%", Utils.locationString(estate.getSignLocation()))
                            .replace("%player%", Bukkit.getOfflinePlayer(estate.getOwner()).getName()), true);

                    Bukkit.getScheduler().runTask(RealEstate.getInstance().plugin, () -> {
                        Utils.sortInventory(estate.getRenter());
                        Utils.sortInventory(estate.getOwner());
                    });
                    continue;
                }
                manager.plugin.getLogger().info("The rent has been ended.");
                manager.getPlayer(estate.getOwner()).manageMessage(Configuration.getConfig().getString("lang.rent-ended")
                        .replace("%location%", Utils.locationString(estate.getSignLocation()))
                        .replace("%player%", Bukkit.getOfflinePlayer(estate.getRenter()).getName()), true);
                manager.getPlayer(estate.getRenter()).manageMessage(Configuration.getConfig().getString("lang.rent-ended-other")
                        .replace("%location%", Utils.locationString(estate.getSignLocation()))
                        .replace("%player%", Bukkit.getOfflinePlayer(estate.getOwner()).getName()), true);
                // they've gone over.

                Bukkit.getScheduler().runTask(RealEstate.getInstance().plugin, () -> {
                    Utils.sortInventory(estate.getRenter());
                    Utils.sortInventory(estate.getOwner());
                    try {
                        estate.setRenter(-1, null, null);
                    } catch (Exception e) {
                        Location loc = estate.getSignLocation();
                        String location = String.format("(%s) %s, %s, %s", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                        manager.plugin.getLogger().error("Invalid Renting Location: " +  location);
                    }
                    estate.setRenter(-1, null, null); // remove the renter?
                });
                continue;
            }
            if (new Date().getTime() < estate.getNextTime()) continue;

            this.handleNextTime(vaultAddon, estate);
            if (estate.getTimesPaid() == 1) {
                EStatePlayer eStatePlayer = manager.getPlayer(estate.getRenter());
                List<Long> reminders = eStatePlayer.getReminders();
                if (!reminders.contains(estate.getClaimID())) {
                    reminders.add(estate.getClaimID());
                    eStatePlayer.setReminders(reminders);

                    Utils.sendMessage(eStatePlayer.getPlayer(), Configuration.getConfig().getString("lang.reminder").replace("%player%", Bukkit.getOfflinePlayer(estate.getOwner()).getName()).replace("%location%", Utils.locationString(estate.getSignLocation())).replace("%price%", Utils.formatNumber(estate.getPrice())));
                }
            }
            estate.setNextTime();
        }
    }

    private void handleNextTime(VaultAddon vaultAddon, RentingEstate estate) {
        vaultAddon.economy.depositPlayer(Bukkit.getOfflinePlayer(estate.getOwner()), estate.getPrice());
        manager.plugin.getLogger().info("Giving back the money to the owner of the claim, cycle just reached the end.");
    }
}
