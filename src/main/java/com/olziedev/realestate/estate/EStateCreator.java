package com.olziedev.realestate.estate;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.estate.rent.RentFlags;
import com.olziedev.realestate.estate.rent.NoExtendRule;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.managers.DatabaseManager;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
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
    private NoExtendRule noExtendRule;
    private String rentFlagError;
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
        try {
            this.price = Long.parseLong(price.replace("$", ""));
        } catch (NumberFormatException ignored) {
            this.price = -1;
        }
    }

    public void setTime(String time) {
        this.time = Utils.parseShortTime(time) * 1000L;
    }

    public void setRentFlags(String rentFlags) {
        this.rentFlags = RentFlags.getByTag(rentFlags);
        if (!NoExtendRule.isRequested(rentFlags, RentFlags.NOEXTEND.getTag())) return;

        if (!this.player.hasPermission("realestate.noextend.create")
                || !this.player.hasPermission("realestate.noextend.bypass")) {
            this.rentFlagError = "lang.noextend-create-denied";
            return;
        }

        this.noExtendRule = NoExtendRule.parse(rentFlags, RentFlags.NOEXTEND.getTag());
        if (this.noExtendRule == null) {
            this.rentFlagError = "lang.invalid-noextend";
            return;
        }

        this.rentFlags.remove(RentFlags.RENEW);
        this.rentFlags.remove(RentFlags.NICEMODE);
        if (!this.rentFlags.contains(RentFlags.NOEXTEND)) {
            this.rentFlags.add(RentFlags.NOEXTEND);
        }
    }

    public void create(Block block, long claimID, long parentID) {
        if (this.rentFlagError != null) {
            String fallback = this.rentFlagError.equals("lang.invalid-noextend")
                    ? "&cInvalid no-extend flag. Use -ne <group> <days>."
                    : "&cYou do not have permission to create non-extendable rents.";
            Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), this.rentFlagError, fallback));
            return;
        }
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
            if (isRent) {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO estate_renting(id, parent_id, owner, time, flags, no_extend_group, no_extend_days, price, sign_world, sign_x, sign_y, sign_z) "
                                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                );
                ps.setLong(1, claimID);
                ps.setLong(2, parentID);
                ps.setString(3, String.valueOf(player.getUniqueId()));
                ps.setLong(4, this.time);
                ps.setString(5, this.rentFlags == null ? null : this.rentFlags.stream().map(RentFlags::name).collect(Collectors.joining(",")));
                ps.setString(6, this.noExtendRule == null ? null : this.noExtendRule.group());
                ps.setInt(7, this.noExtendRule == null ? 0 : this.noExtendRule.cooldownDays());
                ps.setLong(8, this.price);
                ps.setString(9, location.getWorld().getName());
                ps.setInt(10, location.getBlockX());
                ps.setInt(11, location.getBlockY());
                ps.setInt(12, location.getBlockZ());
                ps.executeUpdate();
            } else {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO estate_selling(id, parent_id, owner, price, sign_world, sign_x, sign_y, sign_z) VALUES(?, ?, ?, ?, ?, ?, ?, ?)"
                );
                ps.setLong(1, claimID);
                ps.setLong(2, parentID);
                ps.setString(3, String.valueOf(player.getUniqueId()));
                ps.setLong(4, this.price);
                ps.setString(5, location.getWorld().getName());
                ps.setInt(6, location.getBlockX());
                ps.setInt(7, location.getBlockY());
                ps.setInt(8, location.getBlockZ());
                ps.executeUpdate();
            }
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
