package com.olziedev.antibackteleport.addon;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.olziedev.antibackteleport.AntiBackTeleport;

import java.util.UUID;

public class EssentialsAddon extends Addon {

    private Essentials essentials;

    public EssentialsAddon(AntiBackTeleport plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        this.essentials = Essentials.getPlugin(Essentials.class);
    }

    public User getUser(UUID uuid) {
        return this.essentials.getUser(uuid);
    }
}
