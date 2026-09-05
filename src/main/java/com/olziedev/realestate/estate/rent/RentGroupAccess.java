package com.olziedev.realestate.estate.rent;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.entity.Player;

public final class RentGroupAccess {

    private RentGroupAccess() {}

    public static boolean denyIfBlocked(Player player, RentingEstate target) {
        if (target.hasNoExtend() && !player.hasPermission("realestate.bypass.noextend")) {
            String group = target.getNoExtendGroup();
            if (denyTimedLock(player, group)) return true;
            if (denyActiveRent(player, group)) return true;
        }

        if (target.hasExclusive()) {
            String group = target.getExclusiveGroup();
            if (!player.hasPermission("realestate.bypass.noextend") && denyTimedLock(player, group)) return true;
            if (!player.hasPermission("realestate.bypass.exclusive") && denyActiveRent(player, group)) return true;
        }
        return false;
    }

    private static boolean denyTimedLock(Player player, String group) {
        long blockedUntil = RealEstate.getDatabaseManager().getGroupBlockedUntil(player.getUniqueId(), group);
        long now = System.currentTimeMillis();
        if (blockedUntil <= now) return false;

        long remaining = blockedUntil - now;
        long seconds = Math.max(1L, remaining / 1000L + (remaining % 1000L == 0 ? 0 : 1));
        String message = Configuration.getString(
                Configuration.getConfig(),
                "lang.noextend-blocked",
                "&cYou cannot rent another estate in group %group% for %time%."
        );
        Utils.sendMessage(player, message
                .replace("%group%", group)
                .replace("%time%", Utils.formatTime(seconds)));
        return true;
    }

    private static boolean denyActiveRent(Player player, String group) {
        if (!RealEstate.getDatabaseManager().hasActiveGroupedRent(player.getUniqueId(), group)) return false;

        String message = Configuration.getString(
                Configuration.getConfig(),
                "lang.exclusive-blocked",
                "&cYou cannot rent another estate in group %group% while you are already renting one."
        );
        Utils.sendMessage(player, message.replace("%group%", group));
        return true;
    }
}
