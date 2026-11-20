package com.olziedev.parkour.model;

import java.util.UUID;

/** A single recorded finish on a course's leaderboard (a player's best time). */
public class LeaderboardEntry {

    private final UUID uuid;
    private final String name;
    private final long timeMillis;

    public LeaderboardEntry(UUID uuid, String name, long timeMillis) {
        this.uuid = uuid;
        this.name = name;
        this.timeMillis = timeMillis;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public long getTimeMillis() {
        return timeMillis;
    }
}
