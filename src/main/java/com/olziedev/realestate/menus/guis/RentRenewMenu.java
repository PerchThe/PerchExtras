package com.olziedev.realestate.menus.guis;

import com.olziedev.olziemenu.framework.menu.CachedMenu;
import com.olziedev.olziemenu.framework.menu.FrameworkMenu;
import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.estate.rent.RentFlags;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.managers.Manager;
import com.olziedev.realestate.menus.Menu;
import com.olziedev.realestate.player.GUIPlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;

public class RentRenewMenu extends Menu {

    public RentRenewMenu(Manager manager) {
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
            if (event.getSlot() == section.getInt("renew.slot", -1)) {
                eState.ready = true;
                eState.removeHold(false);
                eState.setRenter(guiPlayer.getAmount(), guiPlayer.getUUID(), Collections.singletonList(RentFlags.RENEW));
                player.closeInventory();
                return true;
            }
            if (event.getSlot() == section.getInt("no-renew.slot", -1)) {
                eState.ready = true;
                eState.removeHold(false);
                eState.setRenter(guiPlayer.getAmount(), guiPlayer.getUUID(), null);
                player.closeInventory();
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

    @Override
    protected ConfigurationSection getSection() {
        return Configuration.getGUI().getConfigurationSection("rentrenew");
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
