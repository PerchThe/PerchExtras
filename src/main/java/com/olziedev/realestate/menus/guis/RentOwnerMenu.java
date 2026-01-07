package com.olziedev.realestate.menus.guis;

import com.olziedev.olziemenu.framework.menu.CachedMenu;
import com.olziedev.olziemenu.framework.menu.FrameworkMenu;
import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.VaultAddon;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.managers.Manager;
import com.olziedev.realestate.menus.Menu;
import com.olziedev.realestate.player.GUIPlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class RentOwnerMenu extends Menu {

    public RentOwnerMenu(Manager manager) {
        super(manager);
    }

    @Override
    public void load() {
        this.cachedMenu = new CachedMenu(this.getSection().getInt("size"), Utils.color(this.getSection().getString("title"))).listenClick((event, menu) -> {
            Player player = (Player) event.getWhoClicked();
            GUIPlayer guiPlayer = RealEstate.getDatabaseManager().getPlayer(player.getUniqueId()).getGUIPlayer();

            RentingEstate eState = (RentingEstate) guiPlayer.getEstate();
            if (eState == null) return true;

            ConfigurationSection section = this.getSection().getConfigurationSection("clickable-items");
            if (event.getSlot() == section.getInt("cancel.slot", -1)) {
                float value = ((float) eState.getPrice() / (eState.getTime() / 1000f));
                double amount = (int) TimeUnit.MILLISECONDS.toSeconds((new Date().getTime() - (eState.getNextTime() - eState.getTime()))) * value;
                VaultAddon vaultAddon = RealEstate.getAddonManager().getAddon(VaultAddon.class);
                if (vaultAddon.economyEnabled()) {
                    vaultAddon.economy.depositPlayer(Bukkit.getOfflinePlayer(eState.getRenter()), amount);
                }
                eState.setRenter(-1, null, null);
                eState.setCancelled(false);
                player.closeInventory();
                Utils.sortInventory(eState.getRenter());
                return true;
            }
            if (event.getSlot() == section.getInt("cycle.slot", -1) && !eState.isCancelled()) {
                eState.setCancelled(true);
                eState.setPaidPrice(-1, eState.getRenter());
                Utils.sendMessage(player, Configuration.getConfig().getString("lang.successfully-ended-owner"));
                player.closeInventory();
                Utils.sortInventory(eState.getRenter());
            }
            return true;
        });
    }

    @Override
    protected ConfigurationSection getSection() {
        return Configuration.getGUI().getConfigurationSection("rentowner");
    }

    @Override
    public void open(Player player) {
        this.kickBed(player);
        FrameworkMenu menu = RealEstate.getMenuManager()
                .getOlzieMenu()
                .createMenu(this.cachedMenu);
        GUIPlayer guiPlayer = RealEstate.getDatabaseManager().getPlayer(player.getUniqueId()).getGUIPlayer();

        RentingEstate rentingEstate = (RentingEstate) guiPlayer.getEstate();
        this.createItems(menu, rentingEstate, getSection(), "items", "clickable-items");
        if (rentingEstate.isCancelled()) {
            menu.setItem(this.getSection().getInt("clickable-items.cycle.slot"), this.createItem(this.getSection().getConfigurationSection("cancelled-item")));
        }
        menu.openInventory(player);
    }
}
