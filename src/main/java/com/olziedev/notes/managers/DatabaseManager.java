package com.olziedev.notes.managers;

import com.olziedev.notes.Notes;
import com.olziedev.notes.note.Note;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager extends Manager {

    private Connection connection; // sqlite connection

    private final Map<UUID, List<Note>> notes = new ConcurrentHashMap<>();

    public DatabaseManager(Notes plugin) {
        super(plugin);
    }

    @Override
    public void setup() {
        try {
            File file = new File(this.plugin.getDataFolder(), "database.db");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            Connection con = this.connect();
            con.prepareStatement("CREATE TABLE IF NOT EXISTS notes_players(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid VARCHAR(255), note LONGTEXT)").execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void load() {
        try {
            Connection con = this.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM notes_players");
            ResultSet result = ps.executeQuery();
            while (result.next()) {
                this.addNote(UUID.fromString(result.getString("uuid")), new Note(result.getLong("id"), result.getString("note")));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            this.connection.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Connection connect() throws Exception {
        Class.forName("org.sqlite.JDBC");
        File file = new File(plugin.getDataFolder(), "database.db");
        return this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    public Connection getConnection() throws Exception {
        boolean closed;
        try {
            closed = !this.connection.isValid(5);
        } catch (Throwable ex) {
            closed = this.connection.isClosed();
        }
        return closed ? this.connect() : this.connection;
    }

    public List<Note> getNotes(UUID uuid) {
        return this.notes.getOrDefault(uuid, new ArrayList<>());
    }

    public void addNote(UUID uuid, Note note) {
        List<Note> notes = this.getNotes(uuid);
        notes.add(note);
        this.notes.put(uuid, notes);
    }

    public void removeNote(UUID uuid, Note note) {
        List<Note> notes = this.getNotes(uuid);
        notes.remove(note);
        this.notes.put(uuid, notes);
    }
}
