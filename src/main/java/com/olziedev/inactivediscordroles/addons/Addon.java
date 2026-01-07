package com.olziedev.inactivediscordroles.addons;

import com.olziedev.inactivediscordroles.InactiveDiscordRoles;

public abstract class Addon {

    protected final InactiveDiscordRoles plugin;

    public Addon(InactiveDiscordRoles plugin) {
        this.plugin = plugin;
    }

    public abstract void load();
}
