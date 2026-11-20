package com.olziedev.safelogin;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SafeLoginListener implements Listener {

    private final JavaPlugin plugin;
    private final Set<String> resourceWorlds = new HashSet<>();
    private FileConfiguration config;
    private SafeLocationFinder locationFinder;

    public SafeLoginListener(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        File folder = new File(plugin.getDataFolder(), "SafeLogin");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File configFile = new File(folder, "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("SafeLogin/config.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.locationFinder = new SafeLocationFinder(this.config);

        this.resourceWorlds.clear();
        List<String> configuredWorlds = this.config.getStringList("resource-worlds");
        for (String worldName : configuredWorlds) {
            if (worldName != null && !worldName.isBlank()) {
                this.resourceWorlds.add(worldName.toLowerCase(Locale.ROOT));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location joinLocation = player.getLocation();

        if (joinLocation.getWorld() == null) {
            return;
        }

        String worldName = joinLocation.getWorld().getName().toLowerCase(Locale.ROOT);
        if (!resourceWorlds.contains(worldName)) {
            return;
        }

        if (!locationFinder.isInDanger(player, joinLocation)) {
            return;
        }

        Location spawnLocation = locationFinder.getConfiguredSpawnLocation(joinLocation.getWorld());
        if (spawnLocation == null) {
            return;
        }

        if (config.getBoolean("reset-states-on-rescue", true)) {
            player.setFallDistance(0.0f);
            player.setFireTicks(0);
            player.setVelocity(new Vector(0, 0, 0));
        }

        player.teleport(spawnLocation);

        String message = config.getString("rescue-message", "");
        if (message != null && !message.isEmpty()) {
            player.sendMessage(SafeLoginMessages.colorize(message));
        }
    }
}