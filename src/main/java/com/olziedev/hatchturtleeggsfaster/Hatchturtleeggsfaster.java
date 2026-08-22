package com.olziedev.hatchturtleeggsfaster;

import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.TurtleEgg;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Turtle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class Hatchturtleeggsfaster extends SpotPlugin implements Listener {

    private static Hatchturtleeggsfaster instance = null;
    private NamespacedKey pdcKey;

    @Override
    public String getName() {
        return "HatchTurtleEggsFaster";
    }

    @Override
    public void onEnable() {
        instance = this;
        pdcKey = new NamespacedKey(this.plugin, "tracked_turtle_eggs");

        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        Bukkit.getScheduler().runTaskTimer(this.plugin, this::processLoadedEggs, 3000L, 3000L);
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this.plugin);
        instance = null;
    }

    private void processLoadedEggs() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {

                if (!chunk.getPersistentDataContainer().has(pdcKey, PersistentDataType.INTEGER_ARRAY)) {
                    continue;
                }

                int[] coords = chunk.getPersistentDataContainer().get(pdcKey, PersistentDataType.INTEGER_ARRAY);
                if (coords == null || coords.length == 0) continue;

                List<Integer> stillValidCoords = new ArrayList<>();
                boolean chunkNeedsUpdate = false;

                for (int i = 0; i < coords.length; i += 3) {
                    Block block = world.getBlockAt(coords[i], coords[i + 1], coords[i + 2]);

                    if (block.getType() != Material.TURTLE_EGG) {
                        chunkNeedsUpdate = true;
                        continue;
                    }

                    if (!canTurtleEggHatch(block)) {
                        addCoordsToList(stillValidCoords, coords[i], coords[i + 1], coords[i + 2]);
                        continue;
                    }

                    TurtleEgg eggData = (TurtleEgg) block.getBlockData();

                    if (Math.random() > 0.4) {
                        int currentHatch = eggData.getHatch();

                        if (currentHatch < eggData.getMaximumHatch()) {
                            eggData.setHatch(currentHatch + 1);
                            block.setBlockData(eggData);
                            world.playSound(block.getLocation(), Sound.ENTITY_TURTLE_EGG_CRACK, 1.0f, 1.0f);
                            world.spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 0.5, 0.5), 15, 0.2, 0.2, 0.2, eggData);

                            addCoordsToList(stillValidCoords, coords[i], coords[i + 1], coords[i + 2]);
                        } else {
                            int eggsAmount = eggData.getEggs();
                            world.playSound(block.getLocation(), Sound.ENTITY_TURTLE_EGG_HATCH, 1.0f, 1.0f);

                            block.setType(Material.AIR);
                            chunkNeedsUpdate = true;

                            for (int e = 0; e < eggsAmount; e++) {
                                Turtle baby = (Turtle) world.spawnEntity(block.getLocation().add(0.5, 0.2, 0.5), EntityType.TURTLE);
                                baby.setBaby();
                            }
                        }
                    } else {
                        addCoordsToList(stillValidCoords, coords[i], coords[i + 1], coords[i + 2]);
                    }
                }

                if (chunkNeedsUpdate) {
                    updateChunkPDC(chunk, stillValidCoords);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEggPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.TURTLE_EGG) {
            addEggToChunk(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTurtleLayEgg(EntityChangeBlockEvent event) {
        if (event.getTo() == Material.TURTLE_EGG) {
            addEggToChunk(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEggBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.TURTLE_EGG) {
            removeEggFromChunk(event.getBlock());
        }
    }

    private void addEggToChunk(Block block) {
        Chunk chunk = block.getChunk();
        List<Integer> currentCoords = getCoordsFromChunk(chunk);

        for (int i = 0; i < currentCoords.size(); i += 3) {
            if (currentCoords.get(i) == block.getX()
                    && currentCoords.get(i + 1) == block.getY()
                    && currentCoords.get(i + 2) == block.getZ()) {
                return;
            }
        }

        currentCoords.add(block.getX());
        currentCoords.add(block.getY());
        currentCoords.add(block.getZ());

        updateChunkPDC(chunk, currentCoords);
    }

    private void removeEggFromChunk(Block block) {
        Chunk chunk = block.getChunk();
        List<Integer> currentCoords = getCoordsFromChunk(chunk);

        for (int i = 0; i < currentCoords.size(); i += 3) {
            if (currentCoords.get(i) == block.getX() && currentCoords.get(i+1) == block.getY() && currentCoords.get(i+2) == block.getZ()) {
                currentCoords.remove(i + 2);
                currentCoords.remove(i + 1);
                currentCoords.remove(i);
                break;
            }
        }

        updateChunkPDC(chunk, currentCoords);
    }

    private List<Integer> getCoordsFromChunk(Chunk chunk) {
        List<Integer> list = new ArrayList<>();
        if (chunk.getPersistentDataContainer().has(pdcKey, PersistentDataType.INTEGER_ARRAY)) {
            int[] coords = chunk.getPersistentDataContainer().get(pdcKey, PersistentDataType.INTEGER_ARRAY);
            if (coords != null) {
                for (int c : coords) list.add(c);
            }
        }
        return list;
    }

    private void updateChunkPDC(Chunk chunk, List<Integer> coordsList) {
        if (coordsList.isEmpty()) {
            chunk.getPersistentDataContainer().remove(pdcKey);
        } else {
            int[] coordsArray = new int[coordsList.size()];
            for (int i = 0; i < coordsList.size(); i++) {
                coordsArray[i] = coordsList.get(i);
            }
            chunk.getPersistentDataContainer().set(pdcKey, PersistentDataType.INTEGER_ARRAY, coordsArray);
        }
    }

    private void addCoordsToList(List<Integer> list, int x, int y, int z) {
        list.add(x);
        list.add(y);
        list.add(z);
    }

    private boolean canTurtleEggHatch(Block eggBlock) {
        Material below = eggBlock.getRelative(BlockFace.DOWN).getType();

        return below == Material.SAND
                || below == Material.RED_SAND;
    }
}