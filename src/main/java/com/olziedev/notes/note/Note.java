package com.olziedev.notes.note;

import com.olziedev.notes.Notes;
import com.olziedev.notes.managers.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

public class Note {

    private final long id;
    private final String note;

    public Note(long id, String note) {
        this.id = id;
        this.note = note;
    }

    public long getID() {
        return this.id;
    }

    public String getNote() {
        return this.note;
    }

    public void delete(UUID uuid) {
        DatabaseManager manager = Notes.getDatabaseManager();
        manager.removeNote(uuid, this);
        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM notes_players WHERE id = ?");
            ps.setLong(1, this.id);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Note create(UUID uuid, String note) {
        long id = -1;
        DatabaseManager manager = Notes.getDatabaseManager();
        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO notes_players(uuid, note) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, String.valueOf(uuid));
            ps.setString(2, note);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) id = rs.getLong(1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (id == -1) return null;

        Note noteObject = new Note(id, note);
        manager.addNote(uuid, noteObject);
        return noteObject;
    }
}
