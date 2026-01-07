package com.olziedev.realestate.managers;

import com.olziedev.realestate.RealEstate;

public abstract class Manager {

    public final RealEstate plugin;

    public Manager(RealEstate plugin) {
        this.plugin = plugin;
    }

    public abstract void load();

    public abstract void setup();

    public void close() {} // default
}
