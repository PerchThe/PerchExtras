package com.olziedev.realestate.managers;

import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.estate.AuctionEstate;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.player.EStatePlayer;
import org.bukkit.Location;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager extends Manager {

    private final Map<Location, EState> estates = new ConcurrentHashMap<>();
    private final Map<UUID, EStatePlayer> players = new ConcurrentHashMap<>();

    private Connection connection; // sqlite connection

    public DatabaseManager(RealEstate plugin) {
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
            con.prepareStatement("CREATE TABLE IF NOT EXISTS estate_selling(id VARCHAR(255), parent_id INT, owner LONGTEXT, price DOUBLE, sign_world LONGTEXT, sign_x INT, sign_y INT, sign_z INT, PRIMARY KEY(id))").execute();
            con.prepareStatement("CREATE TABLE IF NOT EXISTS estate_renting(id VARCHAR(255), parent_id INT, owner LONGTEXT, price DOUBLE, paid_price LONG, next_time LONG, time LONG, cancelled BOOLEAN, renter LONGTEXT, sign_world LONGTEXT, sign_x INT, sign_y INT, sign_z INT, flags LONGTEXT, activated_flags LONGTEXT, PRIMARY KEY(id))").execute();
            con.prepareStatement("CREATE TABLE IF NOT EXISTS estate_players(uuid VARCHAR(255), messages LONGTEXT, reminders LONGTEXT, PRIMARY KEY(uuid))").execute();

            try {
                con.prepareStatement("ALTER TABLE estate_renting ADD COLUMN flags LONGTEXT").execute();
            } catch (Throwable ignored) {}
            try {
                con.prepareStatement("ALTER TABLE estate_renting ADD COLUMN activated_flags LONGTEXT").execute();
            } catch (Throwable ignored) {}
            try {
                con.prepareStatement("ALTER TABLE estate_players ADD COLUMN dismiss_messages LONGTEXT").execute();
            } catch (Throwable ignored) {}
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void load() {
        try {
            Connection con = this.getConnection();
            ResultSet selling = con.prepareStatement("SELECT id, parent_id FROM estate_selling").executeQuery();
            while (selling.next()) {
                EState estate = new AuctionEstate(selling.getLong("id"), selling.getString("parent_id") == null ? selling.getLong("id") : selling.getLong("parent_id"));

                this.estates.put(estate.getSignLocation(), estate);
            }
            ResultSet renting = con.prepareStatement("SELECT id, parent_id FROM estate_renting").executeQuery();
            while (renting.next()) {
                EState estate = new RentingEstate(renting.getLong("id"), renting.getString("parent_id") == null ? renting.getLong("id") : renting.getLong("parent_id"));

                this.estates.put(estate.getSignLocation(), estate);
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

    public Map<Location, EState> getEStates() {
        return this.estates;
    }

    public Map<UUID, EStatePlayer> getPlayers() {
        return this.players;
    }

    public <T extends EState> T getEState(Location id, Class<T> clazz) {
        EState eState = this.estates.get(id);
        if (!(clazz.isInstance(eState))) return null;

        return clazz.cast(eState);
    }

    public <T extends EState> T getEState(long id, Class<T> clazz) {
        EState eState = this.estates.values().stream().filter(x -> x.getClaimID() == id).findFirst().orElse(null);
        if (!(clazz.isInstance(eState))) return null;

        return clazz.cast(eState);
    }

    public EStatePlayer getPlayer(UUID id) {
        EStatePlayer player = this.players.get(id);
        if (player == null) {
            player = new EStatePlayer(id);
            this.players.put(id, player);
        }
        return player;
    }
}
