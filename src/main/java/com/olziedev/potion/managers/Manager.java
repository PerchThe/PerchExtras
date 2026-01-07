package com.olziedev.potion.managers;

import com.olziedev.potion.Potion;

public abstract class Manager {

    public final Potion plugin;

    public Manager(Potion plugin) {
        this.plugin = plugin;
    }

    public abstract void load();

    public abstract void setup();

    public void close() {} // default
}
