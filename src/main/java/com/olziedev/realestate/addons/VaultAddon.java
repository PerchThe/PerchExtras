package com.olziedev.realestate.addons;

import com.olziedev.realestate.RealEstate;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultAddon extends Addon {

    public Economy economy;

    public VaultAddon(RealEstate plugin) {
        super(plugin);
    }

    public boolean economyEnabled() {
        return economy != null;
    }

    @Override
    public void load() {
        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) economy = economyProvider.getProvider();
    }
}
