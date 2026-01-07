package com.olziedev.inactivediscordroles.role;

import com.earth2me.essentials.User;
import com.olziedev.inactivediscordroles.InactiveDiscordRoles;
import com.olziedev.inactivediscordroles.addons.DiscordSRVAddon;
import com.olziedev.inactivediscordroles.addons.EssentialsAddon;
import com.olziedev.inactivediscordroles.managers.AddonManager;
import com.olziedev.inactivediscordroles.managers.RoleManager;
import com.olziedev.inactivediscordroles.utils.Configuration;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class RoleTimer extends BukkitRunnable {

    private final DiscordSRVAddon discordSRVAddon;
    private final EssentialsAddon essentialsAddon;

    private final RoleManager roleManager;

    private final long inactiveTime = Configuration.getConfig().getLong("settings.inactive-time");

    public RoleTimer() {
        AddonManager addonManager = InactiveDiscordRoles.getAddonManager();
        this.discordSRVAddon = addonManager.getAddon(DiscordSRVAddon.class);
        this.essentialsAddon = addonManager.getAddon(EssentialsAddon.class);
        this.roleManager = InactiveDiscordRoles.getRoleManager();
    }

    @Override
    public void run() {
        for (UUID uuid : discordSRVAddon.getRegisteredUsers()) {
            User user = essentialsAddon.getUser(uuid);
            if (user == null) continue;

            long logout = user.getLastLogout();
            if (user.isVanished() || user.isHidden() || this.isVanished(user.getBase()) || user.getBase().isOnline() || System.currentTimeMillis() - logout < inactiveTime) {
                continue;
            }

            Long id = this.discordSRVAddon.getIDFromUUID(uuid);
            if (id == null) {
                continue;
            }
            List<InactiveRole> inactiveRoles = roleManager.getRole(id);
            if (inactiveRoles.isEmpty()) {
                continue;
            }
            inactiveRoles.forEach(inactiveRole -> {
                Role inactiveRoleObject = inactiveRole.getInactiveRoleObject();
                if (inactiveRoleObject == null) {
                    return;
                }
                inactiveRole.handleRole(inactiveRoleObject.getGuild().getMemberById(id), true);
            });
        }
    }

    private boolean isVanished(Player player) {
        if (player == null) return false;

        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}
