package com.olziedev.realestate.menus.guis;

import com.olziedev.olziemenu.framework.menu.CachedMenu;
import com.olziedev.olziemenu.framework.menu.FrameworkMenu;
import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.managers.Manager;
import com.olziedev.realestate.menus.Menu;
import com.olziedev.realestate.player.GUIPlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class RentHoldOwnerMenu extends Menu {

    public RentHoldOwnerMenu(Manager manager) {
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
            if (event.getSlot() != section.getInt("removehold.slot", -1)) return true;

            eState.removeHold(true);
            player.closeInventory();
            return true;
        });
    }

    @Override
    protected ConfigurationSection getSection() {
        return Configuration.getGUI().getConfigurationSection("rentholdowner");
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
