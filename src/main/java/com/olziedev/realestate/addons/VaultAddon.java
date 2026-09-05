package com.olziedev.realestate.addons;

import com.olziedev.realestate.RealEstate;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultAddon extends Addon {

    public Economy economy;
    public Permission permission;

    public VaultAddon(RealEstate plugin) {
        super(plugin);
    }

    public boolean economyEnabled() {
        return economy != null;
    }

    public boolean hasPermission(OfflinePlayer offlinePlayer, String permissionNode) {
        Player onlinePlayer = offlinePlayer.getPlayer();
        if (onlinePlayer != null) return onlinePlayer.hasPermission(permissionNode);
        if (this.permission != null) return this.permission.playerHas(null, offlinePlayer, permissionNode);
        return offlinePlayer.isOp();
    }

    @Override
    public void load() {
        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) economy = economyProvider.getProvider();

        RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) permission = permissionProvider.getProvider();
    }
}
