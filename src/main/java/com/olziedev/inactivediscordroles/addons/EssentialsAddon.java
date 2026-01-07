package com.olziedev.inactivediscordroles.addons;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.olziedev.inactivediscordroles.InactiveDiscordRoles;

import java.util.UUID;

public class EssentialsAddon extends Addon {

    private Essentials essentials;

    public EssentialsAddon(InactiveDiscordRoles plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        this.essentials = Essentials.getPlugin(Essentials.class);
    }

    public User getUser(UUID uuid) {
        return essentials.getUser(uuid);
    }
}
