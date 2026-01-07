package com.olziedev.invisibleitemframes.commands.subcommands;

import com.olziedev.invisibleitemframes.InvisibleItemFrames;
import com.olziedev.invisibleitemframes.managers.DatabaseManager;
import com.olziedev.invisibleitemframes.utils.Configuration;
import com.olziedev.invisibleitemframes.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkSubCommand;
import org.bukkit.entity.Player;

public class ToggleCommand extends FrameworkSubCommand {

    private final DatabaseManager manager = InvisibleItemFrames.getDatabaseManager();

    public ToggleCommand() {
        super("toggle");
        this.setParent("invisibleitemframes");
        this.setPermissions("invisibleitemframes.toggle");
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();
        if (!manager.getToggled().contains(player.getUniqueId())) {
            manager.setToggled(player.getUniqueId());
            Utils.sendMessage(player, Configuration.getConfig().getString("lang.toggle-on"));
            return;
        }
        manager.removeToggled(player.getUniqueId());
        Utils.sendMessage(player, Configuration.getConfig().getString("lang.toggle-off"));
    }
}
