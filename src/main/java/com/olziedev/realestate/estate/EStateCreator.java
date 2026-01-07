package com.olziedev.realestate.estate;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.estate.rent.RentFlags;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.managers.DatabaseManager;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;

public class EStateCreator {

    private long time;
    private long price;
    private List<RentFlags> rentFlags;
    private final Player player;

    private final String type;
    private final List<String> linesNeeded;
    private final List<String> finishedLines;

    public EStateCreator(String type, Player player) {
        this.type = type;
        this.player = player;
        this.linesNeeded = Configuration.getConfig().getStringList("settings.lines-" + type).stream().map(Utils::color).collect(Collectors.toList());
        this.finishedLines = Configuration.getConfig().getStringList("settings.lines-" + type + "-finished").stream().map(Utils::color).collect(Collectors.toList());
    }

    public void setPrice(String price) {
        this.price = NumberUtils.toLong(price.replace("$", ""), -1);
    }

    public void setTime(String time) {
        this.time = Utils.parseShortTime(time) * 1000L;
    }

    public void setRentFlags(String rentFlags) {
        this.rentFlags = RentFlags.getByTag(rentFlags);
    }

    public void create(Block block, long claimID, long parentID) {
        if (this.price <= 0 || this.time <= 0) return;

        Location location = block.getLocation();
        DatabaseManager manager = RealEstate.getDatabaseManager();

        if (manager.getEState(claimID, EState.class) != null) {
            Utils.sendMessage(player, Configuration.getConfig().getString("lang.already-estate"));
            return;
        }
        try {
            boolean isRent = this.type.equals("rent");
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO estate_" + this.type + "ing(id, parent_id, owner, " + (isRent ? "time, flags," : "") + " price, sign_world, sign_x, sign_y, sign_z) VALUES(?, ?, ?, ?, ?, ?, ?, ?" + (isRent ? ", ?, ?" : "") + ")");
            ps.setLong(1, claimID);
            ps.setLong(2, parentID);
            ps.setString(3, String.valueOf(player.getUniqueId()));
            if (isRent) {
                ps.setLong(4, this.time);
                ps.setString(5, this.rentFlags == null ? null : this.rentFlags.stream().map(RentFlags::name).collect(Collectors.joining(",")));
            }
            int position = isRent ? 6 : 4;
            ps.setLong(position, this.price);
            ps.setString(position + 1, location.getWorld().getName());
            ps.setInt(position + 2, location.getBlockX());
            ps.setInt(position + 3, location.getBlockY());
            ps.setInt(position + 4, location.getBlockZ());
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        EState eState;
        if (this.type.equals("rent")) eState = new RentingEstate(claimID, parentID);
        else eState = new AuctionEstate(claimID, parentID);

        manager.getEStates().put(location, eState);
        Utils.sendMessage(player, Configuration.getConfig().getString("lang.placed-estate"));
        Bukkit.getScheduler().runTaskLater(manager.plugin.plugin, () -> this.update(block, eState), 2L);
    }

    private void update(Block block, EState eState) {
        BlockState blockState = block.getState();
        if (!(blockState instanceof Sign)) return;

        Sign sign = (Sign) blockState;
        int width = eState.getClaim().getWidth();
        for (int i = 0; i < finishedLines.size(); i++) {
            sign.setLine(i, finishedLines.get(i)
                    .replace("%player%", this.player.getName())
                    .replace("%price%", Utils.formatNumber(this.price))
                    .replace("%size%", Utils.formatNumber(width) + "x" + Utils.formatNumber(width))
                    .replace("%flags%", this.rentFlags == null ? "" : this.rentFlags.stream().map(RentFlags::getDisplay).collect(Collectors.joining(", ")))
                    .replace("%time%", Utils.formatTime(this.time / 1000)));
        }
        sign.update();
    }

    public List<String> getLinesNeeded() {
        return this.linesNeeded;
    }

    public List<String> getFinishedLines() {
        return this.finishedLines;
    }
}
