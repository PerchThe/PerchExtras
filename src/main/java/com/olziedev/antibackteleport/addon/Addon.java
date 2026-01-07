package com.olziedev.antibackteleport.addon;

import com.olziedev.antibackteleport.AntiBackTeleport;

public abstract class Addon {

    protected final AntiBackTeleport plugin;

    public Addon(AntiBackTeleport plugin) {
        this.plugin = plugin;
    }

    public abstract void load();
}
