package com.olziedev.notes.managers;

import com.olziedev.notes.Notes;

public abstract class Manager {

    public final Notes plugin;

    public Manager(Notes plugin) {
        this.plugin = plugin;
    }

    public abstract void load();

    public abstract void setup();

    public void close() {} // default
}
