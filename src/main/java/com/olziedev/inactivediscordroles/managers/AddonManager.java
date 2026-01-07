package com.olziedev.inactivediscordroles.managers;

import com.olziedev.inactivediscordroles.InactiveDiscordRoles;
import com.olziedev.inactivediscordroles.addons.Addon;
import com.olziedev.inactivediscordroles.addons.DiscordSRVAddon;
import com.olziedev.inactivediscordroles.addons.EssentialsAddon;

import java.util.ArrayList;
import java.util.List;

public class AddonManager extends Manager {

    private final List<Addon> addons;

    public AddonManager(InactiveDiscordRoles plugin) {
        super(plugin);
        this.addons = new ArrayList<>();
    }

    @Override
    public void setup() {
        addons.add(new EssentialsAddon(plugin));
        addons.add(new DiscordSRVAddon(plugin));
        addons.forEach(Addon::load);
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
