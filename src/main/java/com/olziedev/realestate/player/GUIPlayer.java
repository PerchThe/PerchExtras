package com.olziedev.realestate.player;

import com.olziedev.realestate.estate.EState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GUIPlayer {

    private final UUID uuid;
    private Integer amount;
    private EState eState;
    private boolean dontReady;

    public GUIPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    public EState getEstate() {
        return this.eState;
    }

    public void setEstate(EState estate) {
        this.eState = estate;
    }

    public Integer getAmount() {
        return this.amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public boolean isDontReady() {
        return dontReady;
    }

    public void setDontReady(boolean dontReady) {
        this.dontReady = dontReady;
    }
}
