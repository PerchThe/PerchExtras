package com.olziedev.inactivediscordroles.addons;

import com.olziedev.inactivediscordroles.InactiveDiscordRoles;
import com.olziedev.inactivediscordroles.events.JoinEvent;
import com.olziedev.inactivediscordroles.role.RoleTimer;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.UUID;

public class DiscordSRVAddon extends Addon implements Listener {

    private AccountLinkManager manager;

    public DiscordSRVAddon(InactiveDiscordRoles plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        DiscordSRV.api.subscribe(this);
    }

    @Subscribe
    public void onAccountLink(DiscordReadyEvent event) {
        plugin.getLogger().info("Found DiscordSRV");
        manager = DiscordSRV.getPlugin().getAccountLinkManager();
        plugin.getLogger().info("Link manager: " + manager);
        Bukkit.getPluginManager().registerEvents(new JoinEvent(), plugin.plugin);
        plugin.getLogger().info("Registering event!");
        new RoleTimer().runTaskTimerAsynchronously(plugin.plugin, 0, 20 * 60 * 10);
    }

    public Collection<UUID> getRegisteredUsers() {
        return manager.getLinkedAccounts().values();
    }

    public Long getIDFromUUID(UUID uuid) {
        String id = manager.getDiscordId(uuid);
        if (id == null) return null;

        return Long.parseLong(id);
    }
}
