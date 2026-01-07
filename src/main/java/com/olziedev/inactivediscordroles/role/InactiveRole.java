package com.olziedev.inactivediscordroles.role;

import com.olziedev.inactivediscordroles.InactiveDiscordRoles;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.util.DiscordUtil;

public class InactiveRole {

    private final long role;
    private final long inactiveRole;

    public InactiveRole(long role, long inactiveRole) {
        this.role = role;
        this.inactiveRole = inactiveRole;
    }

    public long getRole() {
        return this.role;
    }

    public long getInactiveRole() {
        return this.inactiveRole;
    }

    public Role getRoleObject() {
        return DiscordUtil.getJda().getRoleById(this.role);
    }

    public Role getInactiveRoleObject() {
        return DiscordUtil.getJda().getRoleById(this.inactiveRole);
    }

    public void handleRole(Member member, boolean forInactive) {
        if (member == null) return;

        Role inactiveRole = this.getInactiveRoleObject();
        Role role = this.getRoleObject();
        if (inactiveRole != null && member.getRoles().contains(inactiveRole) && !forInactive) {
            member.getGuild().removeRoleFromMember(member, inactiveRole).queue();
            if (role != null) member.getGuild().addRoleToMember(member, role).queue();

            InactiveDiscordRoles.getInstance().getLogger().info("REMOVING INACTIVE ROLE FOR MEMBER: " + member.getId());
        }

        if (role != null && member.getRoles().contains(role) && forInactive) {
            member.getGuild().removeRoleFromMember(member, role).queue();
            if (inactiveRole != null) member.getGuild().addRoleToMember(member, inactiveRole).queue();

            InactiveDiscordRoles.getInstance().getLogger().info("ADDING ROLE FOR MEMBER: " + member.getId());
        }
    }
}
