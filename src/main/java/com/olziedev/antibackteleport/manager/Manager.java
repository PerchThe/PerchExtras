package com.olziedev.antibackteleport.manager;

import com.olziedev.antibackteleport.AntiBackTeleport;

public abstract class Manager {

    public final AntiBackTeleport plugin;

    public Manager(AntiBackTeleport plugin) {
        this.plugin = plugin;
    }

    public abstract void load();

    public abstract void setup();

    public void close() {} // default
}
