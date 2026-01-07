package com.olziedev.invisibleitemframes.managers;

import com.olziedev.invisibleitemframes.InvisibleItemFrames;

public abstract class Manager {

    public final InvisibleItemFrames plugin;

    public Manager(InvisibleItemFrames plugin) {
        this.plugin = plugin;
    }

    public abstract void load();

    public abstract void setup();

    public void close() {} // default
}
