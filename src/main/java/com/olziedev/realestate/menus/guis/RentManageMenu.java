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

public class RentManageMenu extends Menu {

    public RentManageMenu(Manager manager) {
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
            VaultAddon vaultAddon = RealEstate.getAddonManager().getAddon(VaultAddon.class);
            double price = eState.getPrice();
            if (event.getSlot() == section.getInt("unrent.slot", -1)) {
                if (vaultAddon.economyEnabled()) {
                    if (vaultAddon.economy.getBalance(player) < price) {
                        Utils.sendMessage(player, Configuration.getConfig().getString("lang.not-enough-rent").replace("%price%", Utils.formatNumber(price)));
                        return true;
                    }
                    vaultAddon.economy.withdrawPlayer(player, price);
                    vaultAddon.economy.depositPlayer(Bukkit.getOfflinePlayer(eState.getOwner()), price + ((eState.getTimesPaid()) * price));
                }
                eState.setRenter(-1, null, null);
                Utils.sendMessage(player, Configuration.getConfig().getString("lang.successfully-unrented"));
                player.closeInventory();
                Utils.sortInventory(eState.getOwner());
                return true;
            }
            if (event.getSlot() == section.getInt("cancel.slot", -1) && eState.getPaidPrice() != eState.getNextTime()) {
                vaultAddon.economy.depositPlayer(Bukkit.getOfflinePlayer(eState.getRenter()), (eState.getTimesPaid() - 1) * price);
                eState.setCancelled(true);
                eState.setPaidPrice(-1, eState.getRenter());
                player.closeInventory();
                Utils.sendMessage(player, Configuration.getConfig().getString("lang.successfully-ended"));
                Utils.sortInventory(eState.getOwner());
                return true;
            }
            if (event.getSlot() == section.getInt("cycle.slot", -1) && !eState.isCancelled()) RealEstate.getMenuManager().getMenu(RentCycleMenu.class).handleSign(eState, guiPlayer, false);
            return true;
        });
    }

    @Override
    protected ConfigurationSection getSection() {
        return Configuration.getGUI().getConfigurationSection("rentmanage");
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
