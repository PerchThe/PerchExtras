package com.olziedev.realestate.events;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.addons.GriefPreventionAddon;
import com.olziedev.realestate.estate.EStateCreator;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlaceEvent implements Listener {

    private final GriefPreventionAddon griefPrevention;

    public PlaceEvent() {
        this.griefPrevention = RealEstate.getAddonManager().getAddon(GriefPreventionAddon.class);
    }

    @EventHandler
    public void onSignCreate(SignChangeEvent event) {
        Claim claim = griefPrevention.getClaim(event.getBlock().getLocation());
        if (claim == null || claim.isAdminClaim()) return;

        List<String> lines = new ArrayList<>(Arrays.asList(event.getLines()));
        String prefix = lines.get(0);
        EStateCreator creator = null;
        switch (prefix.toLowerCase()) {
            case "[rent]":
                creator = new EStateCreator("rent", event.getPlayer());
                creator.setTime(lines.get(creator.getLinesNeeded().indexOf("%time%")));
                creator.setPrice(lines.get(creator.getLinesNeeded().indexOf("%price%")));
                creator.setRentFlags(lines.get(creator.getLinesNeeded().indexOf("%flags%")));
                break;
            case "[sell]":
                creator = new EStateCreator("sell", event.getPlayer());
                creator.setPrice(lines.get(creator.getLinesNeeded().indexOf("%price%")));
                creator.setTime("1s"); // bypass the check in the creator
                break;
        }
        if (creator == null) return;

        creator.create(event.getBlock(), claim.getID(), claim.parent != null ? claim.parent.getID() : claim.getID());
    }
}
