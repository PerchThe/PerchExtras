package com.olziedev.bulkmapart;

import com.olziedev.spotextras.api.SpotPlugin;
import org.bukkit.command.PluginCommand;

import java.util.Objects;

public final class BulkMapart extends SpotPlugin {

    @Override
    public String getName() {
        return "bulkmapart";
    }

    @Override
    public void onEnable() {
        Messages messages = new Messages(this);
        try {
            messages.loadInitial();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load bulkmapart/messages.yml", exception);
        }

        PluginCommand command = Objects.requireNonNull(
                plugin.getCommand("bulkmapart"),
                "The bulkmapart command is missing from plugin.yml"
        );
        BulkMapartCommand handler = new BulkMapartCommand(plugin, messages);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    @Override
    public void onDisable() {
        // The module has no scheduled tasks or listeners to clean up.
    }
}
