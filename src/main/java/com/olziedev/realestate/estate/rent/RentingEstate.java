package com.olziedev.realestate.estate.rent;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.QuickShopAddon;
import com.olziedev.realestate.addons.VaultAddon;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.player.EStatePlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RentingEstate extends EState {

    private long time;
    private long paidPrice;
    private long nextTime;
    private UUID renter;
    private boolean cancelled;
    private List<RentFlags> rentFlags;
    private List<RentFlags> activatedFlags;

    public RentingEstate(long claimID, long childClaimID) {
        super(claimID, childClaimID);

        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM estate_renting WHERE id = ?");
            ps.setLong(1, claimID);
            ResultSet result = ps.executeQuery();
            while (result.next()) {
                this.innit(result);
                this.time = result.getLong("time");
                this.paidPrice = result.getLong("paid_price");
                this.nextTime = result.getLong("next_time");
                this.renter = result.getString("renter") == null ? null : UUID.fromString(result.getString("renter"));
                this.cancelled = result.getBoolean("cancelled");
                this.rentFlags = RentFlags.parse(result.getString("flags"));
                this.activatedFlags = RentFlags.parse(result.getString("activated_flags"));
            }
            this.trustPlayer();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public long getTime() {
        return this.time;
    }

    public UUID getRenter() {
        return this.renter;
    }

    public String getRenterName() {
        return Bukkit.getOfflinePlayer(this.renter).getName();
    }

    public void setRenter(int timesPaid, UUID renter, List<RentFlags> activatedFlags) {
        this.setRenter(timesPaid, renter, activatedFlags, false);
    }

    public void setRenter(int timesPaid, UUID renter, List<RentFlags> activatedFlags, boolean removeStuff) {
        Claim claim = this.getClaim();
        if (this.renter != null && renter == null) {
            if (removeStuff) {
                RealEstate.getAddonManager().getAddon(QuickShopAddon.class).removeAllShops(claim, this.renter);
            }
            claim.managers.remove(this.renter.toString());
            claim.dropPermission(this.renter.toString());
            this.setPaidPrice(0, this.renter);
            this.setCancelled(false);
            this.updateSign();
            EStatePlayer eStatePlayer = manager.getPlayer(this.renter);
            List<Long> reminders = eStatePlayer.getReminders();
            reminders.remove(this.claimID);
            eStatePlayer.setReminders(reminders);
        }

        this.renter = renter;
        this.activatedFlags = activatedFlags == null ? new ArrayList<>() : activatedFlags;
        if (renter != null) {
            this.trustPlayer();
            Utils.sendMessage(Bukkit.getPlayer(renter), Configuration.getConfig().getString("lang.successfully-rented"));
            manager.getPlayer(this.owner).manageMessage(Configuration.getConfig().getString("lang.successfully-rented-other").replace("%price%", Utils.formatNumber(this.price * timesPaid)).replace("%player%", this.getRenterName()).replace("%location%", Utils.locationString(this.signLocation)), true);
            this.updateSign();
        }
        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE estate_renting SET renter = ?, activated_flags = ? WHERE id = ?");
            ps.setString(1, renter == null ? null : String.valueOf(renter));
            ps.setString(2, activatedFlags == null ? null : activatedFlags.stream().map(RentFlags::name).collect(Collectors.joining(",")));
            ps.setLong(3, this.claimID);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void trustPlayer() {
        if (this.renter == null) return;

        Claim claim = this.getClaim();
        claim.setPermission(renter.toString(), ClaimPermission.Build);
    }

    public void setHold() {
        if (this.renter == null || this.activatedFlags.contains(RentFlags.NICEMODE)) return;

        Claim claim = this.getClaim();
        claim.setPermission(renter.toString(), ClaimPermission.Inventory);

        List<RentFlags> flags = this.getActivatedFlags();
        flags.add(RentFlags.NICEMODE);
        this.setActivatedFlags(flags);
        this.updateSign();
    }

    public void removeHold(boolean remove) {
        if (this.renter == null || !this.activatedFlags.contains(RentFlags.NICEMODE)) return;

        Claim claim = this.getClaim();
        claim.dropPermission(renter.toString());
        this.setRenter(-1, null, null, remove);
        List<RentFlags> flags = this.getActivatedFlags();
        flags.remove(RentFlags.NICEMODE);
        this.setActivatedFlags(flags);
    }

    public boolean setPaidPrice(int timesPaid, UUID renter) {
        VaultAddon vaultAddon = RealEstate.getAddonManager().getAddon(VaultAddon.class);
        if (timesPaid > 0 && vaultAddon.economyEnabled()) {
            double price = this.price * timesPaid;
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(renter);
            if (vaultAddon.economy.getBalance(offlinePlayer) < price) {
                manager.getPlayer(renter).manageMessage(Configuration.getConfig().getString("lang.not-enough-rent").replace("%price%", Utils.formatNumber(price)), true);
                return false;
            }
            vaultAddon.economy.withdrawPlayer(offlinePlayer, price);
        }
        if (this.renter == null) this.setNextTime();

        this.paidPrice = timesPaid == 0 ? 0 : timesPaid == -1 ? this.nextTime : this.nextTime + (this.time * (this.getTimesPaid() + (timesPaid - 1)));
        this.updateSign();
        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE estate_renting SET paid_price = ? WHERE id = ?");
            ps.setLong(1, this.paidPrice);
            ps.setLong(2, this.claimID);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public long getPaidPrice() {
        return this.paidPrice;
    }

    public void setNextTime() {
        this.nextTime = new Date().getTime() + this.time;
        this.updateSign();

        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE estate_renting SET next_time = ? WHERE id = ?");
            ps.setLong(1, this.nextTime);
            ps.setLong(2, this.claimID);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public long getNextTime() {
        return this.nextTime;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;

        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE estate_renting SET cancelled = ? WHERE id = ?");
            ps.setBoolean(1, cancelled);
            ps.setLong(2, this.claimID);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public long getTimesPaid() {
        long timesPaid = ((this.paidPrice - this.nextTime) / this.time) + 1;
        return timesPaid <= 0 ? 0 : timesPaid;
    }

    public void remove() {
        manager.getEStates().remove(this.signLocation);

        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM estate_renting WHERE id = ?");
            ps.setLong(1, this.claimID);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateSign() {
        Bukkit.getScheduler().runTask(manager.plugin.plugin, () -> {
            BlockState blockState = this.signLocation.getBlock().getState();
            if (!(blockState instanceof Sign)) return;

            Sign sign = (Sign) blockState;
            int width = this.getClaim().getWidth();

            if (this.renter == null) {
                List<String> finishedLines = Configuration.getConfig().getStringList("settings.lines-rent-finished").stream().map(Utils::color).collect(Collectors.toList());
                for (int i = 0; i < finishedLines.size(); i++) {
                    sign.setLine(i, finishedLines.get(i)
                            .replace("%player%", Bukkit.getOfflinePlayer(this.owner).getName())
                            .replace("%price%", Utils.formatNumber(this.price))
                            .replace("%size%", Utils.formatNumber(width) + "x" + Utils.formatNumber(width))
                            .replace("%flags%", this.rentFlags.stream().map(RentFlags::getDisplay).collect(Collectors.joining(", ")))
                            .replace("%time%", Utils.formatTime(this.time / 1000)));
                }
            }
            if (this.renter != null && this.activatedFlags.contains(RentFlags.NICEMODE)) {
                List<String> finishedLines = Configuration.getConfig().getStringList("settings.lines-hold-renting").stream().map(Utils::color).collect(Collectors.toList());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM hh:mm a");
                for (int i = 0; i < finishedLines.size(); i++) {
                    sign.setLine(i, finishedLines.get(i)
                            .replace("%player%", Bukkit.getOfflinePlayer(this.renter).getName())
                            .replace("%price%", Utils.formatNumber(this.price))
                            .replace("%size%", Utils.formatNumber(width) + "x" + Utils.formatNumber(width))
                            .replace("%time%", Utils.formatTime(this.time / 1000))
                            .replace("%flags%", this.activatedFlags.stream().map(RentFlags::getDisplay).collect(Collectors.joining(", ")))
                            .replace("%end%", sdf.format(new Date(this.paidPrice))));
                }
            }
            if (this.renter != null && !this.activatedFlags.contains(RentFlags.NICEMODE)) {
                List<String> finishedLines = Configuration.getConfig().getStringList("settings.lines-rent-renting").stream().map(Utils::color).collect(Collectors.toList());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM hh:mm a");
                for (int i = 0; i < finishedLines.size(); i++) {
                    sign.setLine(i, finishedLines.get(i)
                            .replace("%player%", Bukkit.getOfflinePlayer(this.renter).getName())
                            .replace("%price%", Utils.formatNumber(this.price))
                            .replace("%size%", Utils.formatNumber(width) + "x" + Utils.formatNumber(width))
                            .replace("%time%", Utils.formatTime(this.time / 1000))
                            .replace("%flags%", this.activatedFlags.stream().map(RentFlags::getDisplay).collect(Collectors.joining(", ")))
                            .replace("%end%", sdf.format(new Date(this.paidPrice))));
                }
            }
            sign.update();
        });
    }

    public List<RentFlags> getRentFlags() {
        return this.rentFlags;
    }

    public List<RentFlags> getActivatedFlags() {
        return this.activatedFlags;
    }

    public void setActivatedFlags(List<RentFlags> activatedFlags) {
        this.activatedFlags = activatedFlags;

        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE estate_renting SET activated_flags = ? WHERE id = ?");
            ps.setString(1, activatedFlags.stream().map(RentFlags::name).collect(Collectors.joining(",")));
            ps.setLong(2, this.claimID);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
