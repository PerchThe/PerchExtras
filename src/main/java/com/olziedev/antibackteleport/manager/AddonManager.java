package com.olziedev.antibackteleport.manager;

import com.olziedev.antibackteleport.AntiBackTeleport;
import com.olziedev.antibackteleport.addon.Addon;
import com.olziedev.antibackteleport.addon.EssentialsAddon;
import com.olziedev.antibackteleport.addon.WorldguardAddon;

import java.util.ArrayList;
import java.util.List;

public class AddonManager extends Manager {

    private final List<Addon> addons;

    public AddonManager(AntiBackTeleport plugin) {
        super(plugin);
        this.addons = new ArrayList<>();
    }

    @Override
    public void setup() {
        addons.add(new EssentialsAddon(plugin));
        addons.add(new WorldguardAddon(plugin));
    }

    @Override
    public void load() {
        addons.forEach(Addon::load);
    }

    @SuppressWarnings("unchecked")
    public <T extends Addon> T getAddon(Class<T> clazz) {
        return addons.stream().filter(x -> x.getClass().equals(clazz)).map(x -> (T) x).findFirst().orElse(null);
    }
}
