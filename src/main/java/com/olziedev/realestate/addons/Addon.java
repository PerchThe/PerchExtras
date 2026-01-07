package com.olziedev.realestate.addons;

import com.olziedev.realestate.RealEstate;
import org.bukkit.event.Listener;

public abstract class Addon implements Listener {

    protected final RealEstate plugin;

    public Addon(RealEstate plugin) {
        this.plugin = plugin;
    }

    public abstract void load();
}
