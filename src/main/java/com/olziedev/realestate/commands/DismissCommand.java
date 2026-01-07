package com.olziedev.realestate.commands;

import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.ExecutorType;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.managers.DatabaseManager;
import com.olziedev.realestate.player.EStatePlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class DismissCommand extends FrameworkCommand {

    private final DatabaseManager manager = RealEstate.getDatabaseManager();

    public DismissCommand() {
        super("dismiss");
        this.setExecutorType(ExecutorType.PLAYER_ONLY);
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();
        EStatePlayer eStatePlayer = manager.getPlayer(player.getUniqueId());
        eStatePlayer.setReminders(new ArrayList<>());
        eStatePlayer.getDismissMessages().clear();
        eStatePlayer.manageDismissMessage("", false);

        Utils.sendMessage(player, Configuration.getConfig().getString("lang.dismissed"));
    }
}
