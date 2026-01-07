package com.olziedev.realestate.estate;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.GriefPreventionAddon;
import com.olziedev.realestate.managers.DatabaseManager;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.util.UUID;

public abstract class EState {

    protected long claimID;
    protected long parentID;
    protected UUID owner;
    protected Location signLocation;
    protected long price;
    public boolean ready = true;

    protected final DatabaseManager manager;
    protected final GriefPreventionAddon griefPrevention;

    public EState(long claimID, long parentID) {
        this.claimID = claimID;
        this.parentID = parentID;
        this.manager = RealEstate.getDatabaseManager();
        this.griefPrevention = RealEstate.getAddonManager().getAddon(GriefPreventionAddon.class);
    }

    public void innit(ResultSet result) throws Exception {
        this.owner = UUID.fromString(result.getString("owner"));
        this.signLocation = new Location(Bukkit.getWorld(result.getString("sign_world")), result.getInt("sign_x"), result.getInt("sign_y"), result.getInt("sign_z"));
        this.price = result.getLong("price");
    }

    public UUID getOwner() {
        return this.owner;
    }

    public String getOwnerName() {
        if (this.owner == null) {
            return "N/A";
        }
        return Bukkit.getOfflinePlayer(this.owner).getName();
    }

    public long getClaimID() {
        return this.claimID;
    }

    public Claim getClaim() {
        Claim claim = griefPrevention.getClaim(this.parentID);
        Claim subClaim = claim.children.stream().filter(x -> x.getID().equals(this.claimID)).findFirst().orElse(null);
        if (subClaim != null) return subClaim;

        return claim;
    }

    public Claim getParentClaim() {
        return griefPrevention.getClaim(this.parentID);
    }

    public Location getSignLocation() {
        return this.signLocation;
    }

    public long getPrice() {
        return this.price;
    }

    public abstract void remove();
}
