package com.olziedev.safelogin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SafeLocationFinder {

    private final FileConfiguration config;

    public SafeLocationFinder(FileConfiguration config) {
        this.config = config;
    }

    public boolean isInDanger(Player player, Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return false;
        }

        boolean checkSuffocation = config.getBoolean("hazards.suffocation", true);
        boolean checkLava = config.getBoolean("hazards.lava", true);
        boolean checkFire = config.getBoolean("hazards.fire", true);
        boolean checkFallOrVoid = config.getBoolean("hazards.fall-or-void", true);
        double fallThreshold = config.getDouble("hazards.fall-distance-threshold", 8.0);

        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block eye = player.getEyeLocation().getBlock();

        if (checkSuffocation) {
            if (isSolidSuffocating(feet) || isSolidSuffocating(head) || isSolidSuffocating(eye)) {
                return true;
            }
        }

        if (checkLava) {
            if (isLava(feet) || isLava(head) || isLava(eye)) {
                return true;
            }
        }

        if (checkFire) {
            if (player.getFireTicks() > 0 || isFire(feet) || isFire(head)) {
                return true;
            }
        }

        if (checkFallOrVoid) {
            if (loc.getY() < world.getMinHeight()) {
                return true;
            }
            if (player.getFallDistance() >= fallThreshold && !feet.isSolid()) {
                return true;
            }
        }

        return false;
    }

    public Location getConfiguredSpawnLocation(World fallbackWorld) {
        String configuredWorldName = config.getString("spawn-target.world", "");
        World targetWorld = fallbackWorld;

        if (configuredWorldName != null && !configuredWorldName.isBlank()) {
            World resolved = Bukkit.getWorld(configuredWorldName);
            if (resolved != null) {
                targetWorld = resolved;
            }
        }

        if (targetWorld == null) {
            return null;
        }

        double x = config.getDouble("spawn-target.x", 6.5);
        double y = config.getDouble("spawn-target.y", 97.0);
        double z = config.getDouble("spawn-target.z", -229.5);
        float yaw = (float) config.getDouble("spawn-target.yaw", 0.0);
        float pitch = (float) config.getDouble("spawn-target.pitch", 0.0);

        return new Location(targetWorld, x, y, z, yaw, pitch);
    }

    private boolean isSolidSuffocating(Block block) {
        Material type = block.getType();
        return type.isSolid() && type.isOccluding() && !block.isPassable();
    }

    private boolean isLava(Block block) {
        return block.getType() == Material.LAVA;
    }

    private boolean isFire(Block block) {
        Material type = block.getType();
        return type == Material.FIRE || type == Material.SOUL_FIRE;
    }
}