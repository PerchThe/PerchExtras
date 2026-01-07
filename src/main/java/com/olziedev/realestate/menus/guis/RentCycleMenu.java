package com.olziedev.realestate.menus.guis;

import com.olziedev.olziemenu.framework.menu.CachedMenu;
import com.olziedev.olziemenu.framework.menu.FrameworkMenu;
import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.ProtocolLibAddon;
import com.olziedev.realestate.addons.VaultAddon;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.estate.rent.RentFlags;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.managers.Manager;
import com.olziedev.realestate.menus.Menu;
import com.olziedev.realestate.player.GUIPlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class RentCycleMenu extends Menu {

    public RentCycleMenu(Manager manager) {
        super(manager);
    }

    @Override
    public void load() {
        this.cachedMenu = new CachedMenu(this.getSection().getInt("size"), Utils.color(this.getSection().getString("title"))).listenClick((event, menu) -> {
            Player player = (Player) event.getWhoClicked();
            GUIPlayer guiPlayer = this.getDatabaseManager().getPlayer(player.getUniqueId()).getGUIPlayer();

            RentingEstate eState = (RentingEstate) guiPlayer.getEstate();
            if (eState == null) return true;

            ConfigurationSection section = this.getSection().getConfigurationSection("clickable-items");
            if (event.getSlot() == section.getInt("rent.slot", -1)) {
                if (!eState.setPaidPrice(1, player.getUniqueId())) return true;

                if (eState.getRentFlags().contains(RentFlags.RENEW)) {
                    guiPlayer.setDontReady(true);
                    guiPlayer.setAmount(1);
                    RealEstate.getMenuManager().getMenu(RentRenewMenu.class).open(player);
                    return true;
                }
                eState.ready = true;
                eState.removeHold(false);
                eState.setRenter(1, guiPlayer.getUUID(), null);
                player.closeInventory();
                return true;
            }
            if (event.getSlot() == section.getInt("cycle.slot", -1)) {
                guiPlayer.setDontReady(true);
                this.handleSign(eState, guiPlayer, true);
            }
            return true;
        }).listenClose((close, menu) -> {
            Player player = (Player) close.getPlayer();
            GUIPlayer guiPlayer = RealEstate.getDatabaseManager().getPlayer(player.getUniqueId()).getGUIPlayer();
            if (guiPlayer.isDontReady()) return false;

            EState eState = guiPlayer.getEstate();
            if (eState == null) return false;

            eState.ready = true;
            return false;
        });
    }

    public void handleSign(RentingEstate eState, GUIPlayer guiPlayer, boolean newRenter) {
        ProtocolLibAddon addon = RealEstate.getAddonManager().getAddon(ProtocolLibAddon.class);
        VaultAddon vault = RealEstate.getAddonManager().getAddon(VaultAddon.class);
        guiPlayer.setAmount(null);
        addon.openAmountSign(Configuration.getConfig().getConfigurationSection("settings.select-sign"), guiPlayer, amount -> {
            if (amount.get() <= 0) amount.set(1);
            if (amount.get() * eState.getPrice() > vault.economy.getBalance(guiPlayer.getPlayer())) {
                Bukkit.getScheduler().runTaskLater(manager.plugin.plugin, () -> handleSign(eState, guiPlayer, newRenter), 1L);
                Utils.sendMessage(guiPlayer.getPlayer(), Configuration.getConfig().getString("lang.not-enough-rent").replace("%price%", Utils.formatNumber(amount.get() * eState.getPrice())));
                return false;
            }
            return true;
        }, () -> {
            if (guiPlayer.getAmount() == null || !eState.setPaidPrice(guiPlayer.getAmount(), guiPlayer.getUUID())) return;

            if (newRenter) {
                if (eState.getRentFlags().contains(RentFlags.RENEW)) {
                    RealEstate.getMenuManager().getMenu(RentRenewMenu.class).open(guiPlayer.getPlayer());
                    return;
                }
                eState.ready = true;
                eState.removeHold(false);
                eState.setRenter(guiPlayer.getAmount(), guiPlayer.getUUID(), null);
                return;
            }
            Player player = guiPlayer.getPlayer();
            if (player == null) return;

            Utils.sendMessage(player, Configuration.getConfig().getString("lang.extended-rent").replace("%cycle%", Utils.formatNumber(guiPlayer.getAmount())));
        });
        guiPlayer.setDontReady(false);
    }

    @Override
    protected ConfigurationSection getSection() {
        return Configuration.getGUI().getConfigurationSection("rentcycle");
    }

    @Override
    public void open(Player player) {
        this.kickBed(player);
        FrameworkMenu menu = RealEstate.getMenuManager()
                .getOlzieMenu()
                .createMenu(this.cachedMenu);
        GUIPlayer guiPlayer = RealEstate.getDatabaseManager().getPlayer(player.getUniqueId()).getGUIPlayer();

        this.createItems(menu, guiPlayer.getEstate(), getSection(), "items", "clickable-items");
        menu.openInventory(player, inv -> guiPlayer.setDontReady(false));
    }
}
