package com.olziedev.realestate.menus.guis;

import com.olziedev.olziemenu.framework.menu.CachedMenu;
import com.olziedev.olziemenu.framework.menu.FrameworkMenu;
import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.VaultAddon;
import com.olziedev.realestate.estate.AuctionEstate;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.managers.Manager;
import com.olziedev.realestate.menus.Menu;
import com.olziedev.realestate.player.GUIPlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class BuyEstateMenu extends Menu {

    public BuyEstateMenu(Manager manager) {
        super(manager);
    }

    @Override
    public void load() {
        this.cachedMenu = new CachedMenu(this.getSection().getInt("size"), Utils.color(this.getSection().getString("title"))).listenClick((event, menu) -> {
            Player player = (Player) event.getWhoClicked();
            GUIPlayer guiPlayer = this.getDatabaseManager().getPlayer(player.getUniqueId()).getGUIPlayer();

            AuctionEstate eState = (AuctionEstate) guiPlayer.getEstate();
            if (eState == null) return true;

            ConfigurationSection section = this.getSection().getConfigurationSection("clickable-items");
            if (event.getSlot() == section.getInt("buy.slot", -1)) {
                double price = eState.getPrice();
                VaultAddon vaultAddon = RealEstate.getAddonManager().getAddon(VaultAddon.class);
                if (vaultAddon.economyEnabled()) {
                    if (vaultAddon.economy.getBalance(player) < price) {
                        Utils.sendMessage(player, Configuration.getConfig().getString("lang.not-enough-buy").replace("%price%", Utils.formatNumber(price)));
                        return true;
                    }
                    vaultAddon.economy.withdrawPlayer(player, price);
                    vaultAddon.economy.depositPlayer(Bukkit.getOfflinePlayer(eState.getOwner()), price);
                }
                eState.buy(player);
                player.closeInventory();
                return true;
            }
            if (event.getSlot() == section.getInt("cancel.slot", -1)) {
                player.closeInventory();
            }
            return true;
        }).listenClose((close, menu) -> {
            Player player = (Player) close.getPlayer();
            GUIPlayer guiPlayer = this.getDatabaseManager().getPlayer(player.getUniqueId()).getGUIPlayer();
            EState eState = guiPlayer.getEstate();
            if (eState == null) return false;

            eState.ready = true;
            return false;
        });
    }

    @Override
    protected ConfigurationSection getSection() {
        return Configuration.getGUI().getConfigurationSection("buyestate");
    }

    @Override
    public void open(Player player) {
        this.kickBed(player);
        FrameworkMenu menu = RealEstate.getMenuManager()
                .getOlzieMenu()
                .createMenu(this.cachedMenu);
        GUIPlayer guiPlayer = RealEstate.getDatabaseManager().getPlayer(player.getUniqueId()).getGUIPlayer();

        this.createItems(menu, guiPlayer.getEstate(), getSection(), "items", "clickable-items");
        menu.openInventory(player);
    }
}
