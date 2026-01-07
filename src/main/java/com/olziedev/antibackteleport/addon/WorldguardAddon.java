package com.olziedev.antibackteleport.addon;

import com.olziedev.antibackteleport.AntiBackTeleport;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;

import java.util.List;
import java.util.stream.Collectors;

public class WorldguardAddon extends Addon {

    public WorldguardAddon(AntiBackTeleport plugin) {
        super(plugin);
    }

    @Override
    public void load() {

    }

    public void getPlayersInRegion(World world, String regionName, Consumer<List<Player>> players) {
        ProtectedRegion region = this.getRegionByName(world, regionName);
        if (region == null) {
            players.accept(null);
            return;
        }

        Location top = new Location(world, region.getMaximumPoint().getX(), region.getMaximumPoint().getY(), region.getMaximumPoint().getZ());
        Location bottom = new Location(world, region.getMinimumPoint().getX(), region.getMinimumPoint().getY(), region.getMinimumPoint().getZ());

        Bukkit.getScheduler().runTask(plugin.plugin, () -> players.accept(world.getNearbyEntities(BoundingBox.of(top, bottom)).stream().filter(x -> x instanceof Player).map(x -> (Player) x).collect(Collectors.toList())));
    }

    public ProtectedRegion getRegionByName(World world, String regionName) {
        try {
            return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)).getRegion(regionName);
        } catch (Throwable ignored) {
        }
        return null;
    }
}
