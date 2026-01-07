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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RentAdminMenu extends Menu {

    public RentAdminMenu(Manager manager) {
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
            if (event.getSlot() == section.getInt("break.slot", -1)) {
                Utils.sendMessage(player, Configuration.getConfig().getString("lang.broke-estate"));
                Utils.sortInventory(eState.getRenter());
                Utils.sortInventory(eState.getOwner());
                Location location = eState.getSignLocation();
                Material material = location.getBlock().getType();
                location.getBlock().setType(Material.AIR);
                location.getWorld().dropItem(location, new ItemStack(material));

                eState.remove();
                return true;
            }
            if (event.getSlot() == section.getInt("remove.slot", -1) && !eState.isCancelled()) {
                Utils.sortInventory(eState.getRenter());
                Utils.sortInventory(eState.getOwner());
                eState.setRenter(-1, null, null);
                player.closeInventory();
            }
            return true;
        });
    }

    @Override
    protected ConfigurationSection getSection() {
        return Configuration.getGUI().getConfigurationSection("rentadmin");
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
        menu.openInventory(player);
    }
}
