package com.olziedev.notes.commands;

import com.olziedev.notes.Notes;
import com.olziedev.notes.managers.DatabaseManager;
import com.olziedev.notes.note.Note;
import com.olziedev.notes.utils.Configuration;
import com.olziedev.notes.utils.Utils;
import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class NotesCommand extends FrameworkCommand {

    private final DatabaseManager manager = Notes.getDatabaseManager();
    private final int PAGE_LENGTH = Configuration.getConfig().getInt("settings.page-length");

    public NotesCommand() {
        super("notes");
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        Player player = (Player) cmd.getSender();
        String[] args = cmd.getArguments();

        int page = 0;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]) - 1;
            } catch (Exception ex) {
                Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.not-a-number"));
                return;
            }
        }
        OfflinePlayer target = player;
        if (args.length > 1 && player.hasPermission("notes.others")) {
            target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(),"lang.no-player-joined"));
                return;
            }
        }
        List<Note> notes = manager.getNotes(target.getUniqueId());
        int pages = (int) Math.ceil((float) notes.size() / PAGE_LENGTH);

        if (page + 1 > pages || page < 0) {
            Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(),"lang.no-page"));
            return;
        }
        Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(),"lang.notes-header"));
        int start = page * PAGE_LENGTH;
        int end = start + PAGE_LENGTH;

        for (int i = start; i < end; i++) {
            if (i >= notes.size()) break;

            Note note = notes.get(i);
            Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.notes-entry").replace("%id%", String.valueOf(i  + 1)).replace("%note%", note.getNote()));
        }
        Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(),"lang.notes-footer"));
    }
}
