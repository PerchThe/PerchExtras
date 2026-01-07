package com.olziedev.inactivediscordroles.events;

import com.olziedev.inactivediscordroles.InactiveDiscordRoles;
import com.olziedev.inactivediscordroles.addons.DiscordSRVAddon;
import com.olziedev.inactivediscordroles.managers.AddonManager;
import com.olziedev.inactivediscordroles.managers.RoleManager;
import com.olziedev.inactivediscordroles.role.InactiveRole;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class JoinEvent implements Listener {

    private final DiscordSRVAddon discordSRVAddon;

    private final RoleManager roleManager;

    public JoinEvent() {
        AddonManager addonManager = InactiveDiscordRoles.getAddonManager();
        this.discordSRVAddon = addonManager.getAddon(DiscordSRVAddon.class);
        this.roleManager = InactiveDiscordRoles.getRoleManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(roleManager.plugin.plugin, () -> {
            Player player = event.getPlayer();
            Long discordID = discordSRVAddon.getIDFromUUID(player.getUniqueId());
            if (discordID == null) return;

            List<InactiveRole> inactiveRoles = roleManager.getRole(discordID);
            if (inactiveRoles.isEmpty()) return;

            inactiveRoles.forEach(inactiveRole -> {
                Role inactiveRoleObject = inactiveRole.getInactiveRoleObject();
                if (inactiveRoleObject == null) return;

                inactiveRole.handleRole(inactiveRoleObject.getGuild().getMemberById(discordID), false);
            });
        });
    }
}
