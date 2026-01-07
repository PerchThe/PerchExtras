package com.olziedev.inactivediscordroles.managers;

import com.olziedev.inactivediscordroles.InactiveDiscordRoles;
import com.olziedev.inactivediscordroles.role.InactiveRole;
import com.olziedev.inactivediscordroles.utils.Configuration;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RoleManager extends Manager {

    private final List<InactiveRole> roles;

    public RoleManager(InactiveDiscordRoles plugin) {
        super(plugin);
        this.roles = new ArrayList<>();
    }

    @Override
    public void setup() {}

    @Override
    public void load() {
        for (String keys : Configuration.getConfig().getConfigurationSection("roles").getKeys(false)) {

            ConfigurationSection section = Configuration.getConfig().getConfigurationSection("roles." + keys);
            this.roles.add(new InactiveRole(section.getLong("role"), section.getLong("inactive-role")));
        }
    }

    public List<InactiveRole> getRoles() {
        return this.roles;
    }

    public List<InactiveRole> getRole(long discordID) {
        return this.getRoles().stream().filter(x -> {
            Role inactiveRole = x.getInactiveRoleObject();
            if (inactiveRole == null) return false;

            Guild guild = inactiveRole.getGuild();
            Member member = guild.getMemberById(discordID);
            if (member == null) return false;

            Role role = x.getRoleObject();
            if (role == null) return false;

            return member.getRoles().contains(inactiveRole) || member.getRoles().contains(role);
        }).collect(Collectors.toList());
    }
}
