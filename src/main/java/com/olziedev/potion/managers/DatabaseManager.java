package com.olziedev.potion.managers;

import com.olziedev.potion.Potion;
import com.olziedev.potion.player.PotionPlayer;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager extends Manager {

    private Connection connection; // sqlite connection

    private final Map<UUID, PotionPlayer> players = new ConcurrentHashMap<>();
    private final Map<String, PotionEffectType> potions = new ConcurrentHashMap<>();

    public DatabaseManager(Potion plugin) {
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
            con.prepareStatement("CREATE TABLE IF NOT EXISTS potion_players(uuid VARCHAR(255), potions LONGTEXT, PRIMARY KEY(uuid))").execute();
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

    public PotionPlayer getPlayer(UUID id) {
        PotionPlayer player = this.players.get(id);
        if (player == null) {
            player = new PotionPlayer(id);
            this.players.put(id, player);
        }
        return player;
    }

    public Map<String, PotionEffectType> getPotions() {
        return this.potions;
    }
}
