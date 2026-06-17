package com.olziedev.lapis;

import org.bukkit.Location;
import java.util.HashSet;
import java.util.Set;

public class TableManager {

    private final Set<Location> inUseTables = new HashSet<>();

    public boolean isLocked(Location loc) {
        return inUseTables.contains(loc);
    }

    public void lock(Location loc) {
        inUseTables.add(loc);
    }

    public void unlock(Location loc) {
        inUseTables.remove(loc);
    }

    public void clearAll() {
        inUseTables.clear();
    }
}
