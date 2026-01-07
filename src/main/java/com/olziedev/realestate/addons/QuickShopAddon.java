package com.olziedev.realestate.addons;

import com.olziedev.realestate.RealEstate;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.api.shop.Shop;

import java.util.Objects;
import java.util.UUID;

public class QuickShopAddon extends Addon {

    private QuickShopAPI quickShopAPI;

    public QuickShopAddon(RealEstate plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("QuickShop");
        if( plugin != null) this.quickShopAPI = (QuickShopAPI) plugin;
    }

    public void removeAllShops(Claim claim, UUID renter) {
        if (this.quickShopAPI == null) return;

        Bukkit.getScheduler().runTask(plugin.plugin, () -> {
        GriefPreventionAddon griefPrevention = RealEstate.getAddonManager().getAddon(GriefPreventionAddon.class);
            for (Shop shop : this.quickShopAPI.getShopManager().getAllShops()) {
                Claim foundClaim = griefPrevention.instance.dataStore.getClaimAt(shop.getLocation(), true, null);
                if (foundClaim == null || !Objects.equals(foundClaim.getID(), claim.getID()) || !shop.getOwner().equals(renter)) continue;

                shop.delete();
            }
        });
    }
}
