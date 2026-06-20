package com.olziedev.preventplayersfromgrabbingtoomanyelytras;

import com.olziedev.spotextras.SpotExtras;
import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.io.File;
import java.io.IOException;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class Preventplayersfromgrabbingtoomanyelytras extends SpotPlugin implements Listener {

    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, UserRecord> userRecords = new HashMap<>();

    private String enabledWorld;
    private int elytraLimit;
    private int headLimit;
    private String elytraMsg;
    private String headMsg;

    @Override
    public String getName() {
        return "PreventPlayersFromGrabbingTooManyElytras";
    }

    @Override
    public void onEnable() {
        loadModuleConfig();
        this.dataFile = new File(getModuleFolder(), "elytra_limits_data.yml");
        loadData();
        Bukkit.getPluginManager().registerEvents(this, SpotExtras.getInstance());
    }

    @Override
    public void onDisable() {
        saveData();
    }

    private File getModuleFolder() {
        File folder = new File(SpotExtras.getInstance().getDataFolder(), "Preventplayersfromgrabbingtoomanyelytras");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private void loadModuleConfig() {
        File configFile = new File(getModuleFolder(), "config.yml");
        if (!configFile.exists()) {
            SpotExtras.getInstance().saveResource("Preventplayersfromgrabbingtoomanyelytras/config.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        this.enabledWorld = config.getString("settings.enabled-world", "ResourceEnd");
        this.elytraLimit = config.getInt("settings.limits.elytra", 1);
        this.headLimit = config.getInt("settings.limits.dragon-head", 1);

        this.elytraMsg = ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.elytra-limit", "&cLimit reached!").replace("{limit}", String.valueOf(this.elytraLimit)));
        this.headMsg = ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.head-limit", "&cLimit reached!").replace("{limit}", String.valueOf(this.headLimit)));
    }

    private boolean tryTakeElytra(Player player) {
        UserRecord record = getRecord(player.getUniqueId());
        if (record.elytrasTaken >= this.elytraLimit) return false;
        record.elytrasTaken++;
        saveData();
        return true;
    }

    private boolean tryTakeDragonHead(Player player) {
        UserRecord record = getRecord(player.getUniqueId());
        if (record.headsTaken >= this.headLimit) return false;
        record.headsTaken++;
        saveData();
        return true;
    }

    private UserRecord getRecord(UUID uuid) {
        YearMonth currentMonth = YearMonth.now();
        UserRecord record = userRecords.computeIfAbsent(uuid, k -> new UserRecord(currentMonth, 0, 0));

        if (!record.lastInteractionMonth.equals(currentMonth)) {
            record.lastInteractionMonth = currentMonth;
            record.elytrasTaken = 0;
            record.headsTaken = 0;
        }
        return record;
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                SpotExtras.getInstance().getLogger().log(Level.SEVERE, "Could not create elytra_limits_data.yml", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        userRecords.clear();

        ConfigurationSection usersSection = dataConfig.getConfigurationSection("users");
        if (usersSection != null) {
            for (String uuidStr : usersSection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String monthStr = usersSection.getString(uuidStr + ".month");
                int elytras = usersSection.getInt(uuidStr + ".elytras", 0);
                int heads = usersSection.getInt(uuidStr + ".heads", 0);

                YearMonth month = YearMonth.parse(monthStr);
                userRecords.put(uuid, new UserRecord(month, elytras, heads));
            }
        }
    }

    private void saveData() {
        dataConfig.set("users", null);

        for (Map.Entry<UUID, UserRecord> entry : userRecords.entrySet()) {
            String path = "users." + entry.getKey().toString();
            UserRecord record = entry.getValue();
            dataConfig.set(path + ".month", record.lastInteractionMonth.toString());
            dataConfig.set(path + ".elytras", record.elytrasTaken);
            dataConfig.set(path + ".heads", record.headsTaken);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            SpotExtras.getInstance().getLogger().log(Level.SEVERE, "Could not save elytra_limits_data.yml", e);
        }
    }

    private boolean isWorldEnabled(World world) {
        return world != null && world.getName().equals(this.enabledWorld);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFramePunch(EntityDamageByEntityEvent event) {
        if (!isWorldEnabled(event.getEntity().getWorld())) return;

        if (event.getEntity() instanceof ItemFrame frame && frame.getItem().getType() == Material.ELYTRA) {
            if (event.getDamager() instanceof Player player) {
                if (!tryTakeElytra(player)) {
                    event.setCancelled(true);
                    player.sendMessage(this.elytraMsg);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameInteract(PlayerInteractEntityEvent event) {
        if (!isWorldEnabled(event.getPlayer().getWorld())) return;

        if (event.getRightClicked() instanceof ItemFrame frame && frame.getItem().getType() == Material.ELYTRA) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameBreakEnvironment(HangingBreakEvent event) {
        if (!isWorldEnabled(event.getEntity().getWorld())) return;

        if (event.getEntity() instanceof ItemFrame frame && frame.getItem().getType() == Material.ELYTRA) {
            if (event.getCause() != HangingBreakEvent.RemoveCause.ENTITY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeadMine(BlockBreakEvent event) {
        if (!isWorldEnabled(event.getBlock().getWorld())) return;

        if (isDragonHead(event.getBlock())) {
            Player player = event.getPlayer();
            if (!tryTakeDragonHead(player)) {
                event.setCancelled(true);
                player.sendMessage(this.headMsg);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!isWorldEnabled(event.getBlock().getWorld())) return;

        if (isDragonHead(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isWorldEnabled(event.getLocation().getWorld())) return;
        event.blockList().removeIf(this::isDragonHead);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isWorldEnabled(event.getBlock().getWorld())) return;
        event.blockList().removeIf(this::isDragonHead);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!isWorldEnabled(event.getBlock().getWorld())) return;

        for (Block block : event.getBlocks()) {
            if (isDragonHead(block)) {
                event.setCancelled(true);
                return;
            }
        }

        Block targetBlock = event.getBlock().getRelative(event.getDirection(), event.getBlocks().size() + 1);
        if (isDragonHead(targetBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!isWorldEnabled(event.getBlock().getWorld())) return;
        for (Block block : event.getBlocks()) {
            if (isDragonHead(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        if (!isWorldEnabled(event.getBlock().getWorld())) return;
        if (isDragonHead(event.getToBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!isWorldEnabled(event.getBlock().getWorld())) return;

        if (isDragonHead(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private boolean isDragonHead(Block block) {
        return block.getType() == Material.DRAGON_HEAD || block.getType() == Material.DRAGON_WALL_HEAD;
    }

    private static class UserRecord {
        YearMonth lastInteractionMonth;
        int elytrasTaken;
        int headsTaken;

        public UserRecord(YearMonth lastInteractionMonth, int elytrasTaken, int headsTaken) {
            this.lastInteractionMonth = lastInteractionMonth;
            this.elytrasTaken = elytrasTaken;
            this.headsTaken = headsTaken;
        }
    }
}