package com.olziedev.realestate.player;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.managers.DatabaseManager;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class EStatePlayer {

    private final UUID uuid;
    private final GUIPlayer guiPlayer;
    private List<String> messages;
    private List<String> dismissMessages;
    private List<Long> reminders;

    private final DatabaseManager manager;

    public EStatePlayer(UUID uuid) {
        this.uuid = uuid;
        this.guiPlayer = new GUIPlayer(uuid);
        this.manager = RealEstate.getDatabaseManager();

        try {
            Connection con = manager.getConnection();
            PreparedStatement create = con.prepareStatement("INSERT OR IGNORE INTO estate_players(uuid) VALUES (?)");
            create.setString(1, String.valueOf(this.uuid));
            create.executeUpdate();

            PreparedStatement ps = con.prepareStatement("SELECT * FROM estate_players WHERE uuid = ?");
            ps.setString(1, String.valueOf(this.uuid));
            ResultSet result = ps.executeQuery();
            while (result.next()) {
                if (result.getString("messages") != null) this.messages = new ArrayList<>(Arrays.asList(result.getString("messages").split("\n")));
                if (result.getString("dismiss_messages") != null) this.dismissMessages = new ArrayList<>(Arrays.asList(result.getString("dismiss_messages").split("\n")));
                if (result.getString("reminders") != null) this.reminders = Arrays.stream(result.getString("reminders").split(", ")).filter(x -> !x.isEmpty()).map(Long::parseLong).collect(Collectors.toList());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public GUIPlayer getGUIPlayer() {
        return this.guiPlayer;
    }

    public List<String> getMessages() {
        return this.messages == null ? Collections.emptyList() : this.messages;
    }

    public void manageMessage(String message, boolean add) {
        List<String> messages = this.messages == null ? new ArrayList<>() : this.messages;
        if (add) {
            Player player = this.getPlayer();
            if (player != null) {
                Utils.sendMessage(player, message);
                return;
            }
            messages.add(message);
        } else {
            messages.remove(message);
        }

        if (messages.isEmpty()) messages = null;
        this.messages = messages;
        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE estate_players SET messages = ? WHERE uuid = ?");
            ps.setString(1, this.messages == null ? null : String.join("\n", this.messages));
            ps.setString(2, String.valueOf(this.uuid));
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<String> getDismissMessages() {
        return this.dismissMessages == null ? Collections.emptyList() : this.dismissMessages;
    }

    public void manageDismissMessage(String message, boolean add) {
        List<String> messages = this.dismissMessages == null ? new ArrayList<>() : this.dismissMessages;
        if (add) {
            Player player = this.getPlayer();
            if (player != null) {
                Utils.sendMessage(player, message);
                return;
            }
            messages.add(message);
        } else messages.remove(message);

        if (messages.isEmpty()) messages = null;
        this.messages = messages;
        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE estate_players SET dismiss_messages = ? WHERE uuid = ?");
            ps.setString(1, this.dismissMessages == null ? null : String.join("\n", this.dismissMessages));
            ps.setString(2, String.valueOf(this.uuid));
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<Long> getReminders() {
        return this.reminders == null ? new ArrayList<>() : this.reminders;
    }

    public void setReminders(List<Long> reminders) {
        this.reminders = reminders;

        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE estate_players SET reminders = ? WHERE uuid = ?");
            ps.setString(1, this.reminders.isEmpty() ? null : this.reminders.stream().map(Object::toString).collect(Collectors.joining(", ")));
            ps.setString(2, String.valueOf(this.uuid));
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
