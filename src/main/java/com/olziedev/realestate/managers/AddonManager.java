package com.olziedev.realestate.managers;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.*;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class AddonManager extends Manager {

    private final List<Addon> addons;

    public AddonManager(RealEstate plugin) {
        super(plugin);
        this.addons = new ArrayList<>();
    }

    @Override
    public void setup() {
        addons.add(new GriefPreventionAddon(plugin));
        addons.add(new VaultAddon(plugin));
        addons.add(new ProtocolLibAddon(plugin));
        addons.add(new QuickShopAddon(plugin));
    }

    @Override
    public void load() {
        addons.forEach(x -> {
            x.load();
            Bukkit.getPluginManager().registerEvents(x, plugin.plugin);
        });
    }

    @SuppressWarnings("unchecked")
    public <T extends Addon> T getAddon(Class<T> clazz) {
        return addons.stream().filter(x -> x.getClass().equals(clazz)).map(x -> (T) x).findFirst().orElse(null);
    }
}
