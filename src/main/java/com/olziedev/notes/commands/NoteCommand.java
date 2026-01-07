package com.olziedev.notes.commands;

import com.olziedev.notes.note.Note;
import com.olziedev.notes.utils.Configuration;
import com.olziedev.notes.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.ExecutorType;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import org.bukkit.entity.Player;

public class NoteCommand extends FrameworkCommand {

    public NoteCommand() {
        super("note");
        this.setExecutorType(ExecutorType.PLAYER_ONLY);
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();
        if (Note.create(player.getUniqueId(), String.join(" ", cmd.getArguments())) == null) return;

        Utils.sendMessage(player, Configuration.getConfig().getString("lang.added-note"));
    }
}
