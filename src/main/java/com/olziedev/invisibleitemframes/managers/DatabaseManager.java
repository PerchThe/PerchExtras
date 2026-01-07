package com.olziedev.invisibleitemframes.managers;

import com.olziedev.invisibleitemframes.InvisibleItemFrames;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager extends Manager {

    private Connection connection; // sqlite connection
    private final List<UUID> toggled = new ArrayList<>();

    public DatabaseManager(InvisibleItemFrames plugin) {
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
            con.prepareStatement("CREATE TABLE IF NOT EXISTS iif_toggled(uuid VARCHAR(255), PRIMARY KEY(uuid))").execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void load() {}

    @Override
    public void close() {
        try {
            this.connection.close();
            this.toggled.clear();
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

    public List<UUID> getToggled() {
        return this.toggled;
    }

    public void setToggled(UUID uuid) {
        this.toggled.add(uuid);

        try {
            PreparedStatement statement = this.getConnection().prepareStatement("INSERT INTO iif_toggled(uuid) VALUES(?)");
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void removeToggled(UUID uuid) {
        this.toggled.remove(uuid);

        try {
            Connection con = this.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM iif_toggled WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loadToggled(UUID uuid) {
        try {
            Connection con = this.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM iif_toggled WHERE uuid = ?");
            ps.setString(1, String.valueOf(uuid));
            ResultSet result = ps.executeQuery();
            if (!result.next()) return;

            this.toggled.add(uuid);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
