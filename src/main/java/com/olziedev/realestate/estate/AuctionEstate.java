package com.olziedev.realestate.estate;

import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuctionEstate extends EState {

    public AuctionEstate(long claimID, long childClaimID) {
        super(claimID, childClaimID);

        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM estate_selling WHERE id = ?");
            ps.setLong(1, claimID);
            ResultSet result = ps.executeQuery();
            while (result.next()) {
                this.innit(result);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void buy(Player player) {
        Block block = this.getSignLocation().getBlock();
        block.setType(Material.AIR);

        Claim claim = this.getParentClaim();
        griefPrevention.instance.dataStore.changeClaimOwner(claim, player.getUniqueId());

        Utils.sendMessage(player, Configuration.getConfig().getString("lang.successfully-bought"));
        manager.getPlayer(this.owner).manageMessage(Configuration.getConfig().getString("lang.successfully-bought-other").replace("%price%", Utils.formatNumber(this.price)).replace("%player%", player.getName()).replace("%location%", Utils.locationString(this.signLocation)), true);
        this.remove();
    }

    public void remove() {
        manager.getEStates().remove(this.signLocation);

        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM estate_selling WHERE id = ?");
            ps.setLong(1, this.claimID);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
