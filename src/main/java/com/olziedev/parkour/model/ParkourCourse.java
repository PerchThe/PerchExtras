package com.olziedev.parkour.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ParkourCourse {

    private final String name;
    private ParkourPoint start;
    private ParkourPoint end;
    private final List<ParkourPoint> checkpoints = new ArrayList<>();
    private final List<String> endCommands = new ArrayList<>();
    private final Map<UUID, LeaderboardEntry> records = new HashMap<>();

    public ParkourCourse(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ParkourPoint getStart() {
        return start;
    }

    public void setStart(ParkourPoint start) {
        this.start = start;
    }

    public ParkourPoint getEnd() {
        return end;
    }

    public void setEnd(ParkourPoint end) {
        this.end = end;
    }

    public List<ParkourPoint> getCheckpoints() {
        return checkpoints;
    }

    public List<String> getEndCommands() {
        return endCommands;
    }

    public Map<UUID, LeaderboardEntry> getRecords() {
        return records;
    }

    public void serialize(ConfigurationSection section) {
        section.set("start", null);
        section.set("end", null);
        section.set("checkpoints", null);

        if (start != null) start.serialize(section.createSection("start"));
        if (end != null) end.serialize(section.createSection("end"));

        List<Object> serializedCheckpoints = new ArrayList<>();
        for (ParkourPoint checkpoint : checkpoints) {
            // Serialize each checkpoint into its own map so ordering is preserved as a list.
            org.bukkit.configuration.MemoryConfiguration temp = new org.bukkit.configuration.MemoryConfiguration();
            checkpoint.serialize(temp);
            serializedCheckpoints.add(temp.getValues(false));
        }
        section.set("checkpoints", serializedCheckpoints);
        section.set("end-commands", new ArrayList<>(endCommands));

        List<Object> serializedRecords = new ArrayList<>();
        for (LeaderboardEntry entry : records.values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("uuid", entry.getUuid().toString());
            map.put("name", entry.getName());
            map.put("time", entry.getTimeMillis());
            serializedRecords.add(map);
        }
        section.set("records", serializedRecords);
    }

    @SuppressWarnings("unchecked")
    public static ParkourCourse deserialize(String name, ConfigurationSection section) {
        ParkourCourse course = new ParkourCourse(name);
        course.setStart(ParkourPoint.deserialize(section.getConfigurationSection("start")));
        course.setEnd(ParkourPoint.deserialize(section.getConfigurationSection("end")));

        for (Object raw : section.getList("checkpoints", new ArrayList<>())) {
            if (!(raw instanceof java.util.Map)) continue;
            org.bukkit.configuration.MemoryConfiguration temp = new org.bukkit.configuration.MemoryConfiguration();
            ((java.util.Map<String, Object>) raw).forEach(temp::set);
            ParkourPoint point = ParkourPoint.deserialize(temp);
            if (point != null) course.checkpoints.add(point);
        }

        course.endCommands.addAll(section.getStringList("end-commands"));

        for (Object raw : section.getList("records", new ArrayList<>())) {
            if (!(raw instanceof java.util.Map)) continue;
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) raw;
            try {
                UUID uuid = UUID.fromString(String.valueOf(map.get("uuid")));
                String playerName = String.valueOf(map.get("name"));
                long time = ((Number) map.get("time")).longValue();
                course.records.put(uuid, new LeaderboardEntry(uuid, playerName, time));
            } catch (Exception ignored) {
            }
        }
        return course;
    }
}
