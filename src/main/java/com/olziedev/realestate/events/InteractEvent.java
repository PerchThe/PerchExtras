package com.olziedev.realestate.events;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.GriefPreventionAddon;
import com.olziedev.realestate.estate.AuctionEstate;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.estate.rent.RentFlags;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.managers.MenuManager;
import com.olziedev.realestate.menus.guis.*;
import com.olziedev.realestate.player.GUIPlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractEvent implements Listener {

    private final GriefPreventionAddon griefPrevention;
    private final MenuManager menuManager;

    public InteractEvent() {
        this.griefPrevention = RealEstate.getAddonManager().getAddon(GriefPreventionAddon.class);
        this.menuManager = RealEstate.getMenuManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Location location = event.getClickedBlock().getLocation();
        Claim claim = griefPrevention.getClaim(location);
        if (claim == null) return;

        EState eState = RealEstate.getDatabaseManager().getEState(location, EState.class);
        if (eState == null) return;

        Player player = event.getPlayer();
        if (!eState.ready) return;

        GUIPlayer guiPlayer = RealEstate.getDatabaseManager().getPlayer(player.getUniqueId()).getGUIPlayer();
        guiPlayer.setEstate(eState);
        if (eState instanceof AuctionEstate) {
            if (griefPrevention.isPart(eState.getParentClaim(), player.getUniqueId(), false)) {
                Utils.sendMessage(player, Configuration.getConfig().getString("lang.cannot-buy"));
                return;
            }
            eState.ready = false;
            menuManager.getMenu(BuyEstateMenu.class).open(player);
            return;
        }
        if (!(eState instanceof RentingEstate)) return;

        RentingEstate rentingEstate = (RentingEstate) eState;
        if (rentingEstate.getActivatedFlags().contains(RentFlags.NICEMODE) && player.getUniqueId().equals(rentingEstate.getRenter())) {
            menuManager.getMenu(RentHoldMenu.class).open(player);
            return;
        }
        if (rentingEstate.getActivatedFlags().contains(RentFlags.NICEMODE) && player.getUniqueId().equals(rentingEstate.getOwner())) {
            menuManager.getMenu(RentHoldOwnerMenu.class).open(player);
            return;
        }
        if (rentingEstate.getRenter() != null && player.getUniqueId().equals(rentingEstate.getRenter())) {
            menuManager.getMenu(RentManageMenu.class).open(player);
            return;
        }
        if (rentingEstate.getRenter() != null && player.getUniqueId().equals(rentingEstate.getOwner())) {
            menuManager.getMenu(RentOwnerMenu.class).open(player);
            return;
        }
        if (rentingEstate.getRenter() != null && player.hasPermission("realestate.admin")) {
            menuManager.getMenu(RentAdminMenu.class).open(player);
            return;
        }
        if (rentingEstate.getRenter() != null && !player.getUniqueId().equals(rentingEstate.getOwner())) {
            Utils.sendMessage(player, Configuration.getConfig().getString("lang.cannot-rent").replace("%player%", rentingEstate.getRenterName()));
            return;
        }
        if (griefPrevention.isPart(rentingEstate.getParentClaim(), player.getUniqueId(), false)) {
            Utils.sendMessage(player, Configuration.getConfig().getString("lang.cannot-buy"));
            return;
        }
        eState.ready = false;
        menuManager.getMenu(RentEstateMenu.class).open(player);
    }
}
