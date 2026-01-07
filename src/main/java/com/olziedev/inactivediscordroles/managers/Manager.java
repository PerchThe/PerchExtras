package com.olziedev.inactivediscordroles.managers;

import com.olziedev.inactivediscordroles.InactiveDiscordRoles;

public abstract class Manager {

    public final InactiveDiscordRoles plugin;

    public Manager(InactiveDiscordRoles plugin) {
        this.plugin = plugin;
    }

    public abstract void load();

    public abstract void setup();

    public void close() {} // default
}
