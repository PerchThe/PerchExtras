package com.olziedev.potion.player;

import com.olziedev.potion.Potion;
import com.olziedev.potion.managers.DatabaseManager;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PotionPlayer {

    private final UUID uuid;
    private List<PotionEffectType> potions;

    private final DatabaseManager manager;

    public PotionPlayer(UUID uuid) {
        this.manager = Potion.getDatabaseManager();
        this.uuid = uuid;
        this.potions = new ArrayList<>();

        try {
            Connection con = manager.getConnection();
            PreparedStatement create = con.prepareStatement("INSERT OR IGNORE INTO potion_players(uuid) VALUES (?)");
            create.setString(1, String.valueOf(this.uuid));
            create.executeUpdate();

            PreparedStatement ps = con.prepareStatement("SELECT * FROM potion_players WHERE uuid = ?");
            ps.setString(1, String.valueOf(this.uuid));
            ResultSet result = ps.executeQuery();
            while (result.next()) {
                String potions = result.getString("potions");
                if (potions == null) continue;

                for (String s : potions.split(", ")) {
                    this.potions.add(PotionEffectType.getByName(s));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public UUID getUUID() {
        return this.uuid;
    }


    public Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    public List<PotionEffectType> getPotions() {
        return this.potions;
    }

    public void setPotions(List<PotionEffectType> potions) {
        this.potions = potions;

        try {
            Connection con = manager.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE potion_players SET potions = ? WHERE uuid = ?");
            ps.setString(1, this.potions.isEmpty() ? null : this.potions.stream().map(PotionEffectType::getName).collect(Collectors.joining(", ")));
            ps.setString(2, String.valueOf(this.uuid));
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void add(PotionEffectType effect) {
        this.potions.add(effect);
        this.setPotions(this.potions);
    }

    public void remove(PotionEffectType effect) {
        this.potions.remove(effect);
        this.setPotions(this.potions);
    }

    public PotionEffect createEffect(PotionEffectType effect) {
        Player player = this.getPlayer();
        List<Integer> limits = new ArrayList<>();
        if (player != null) player.recalculatePermissions();

        for (PermissionAttachmentInfo perms : player == null ? Collections.<PermissionAttachmentInfo>emptyList() : player.getEffectivePermissions()) {
            if (!perms.getPermission().startsWith("potion." + effect.getName().toLowerCase() + ".")) continue;

            String[] value = perms.getPermission().split("potion." + effect.getName().toLowerCase() + ".");
            if (value.length > 1 && NumberUtils.isDigits(value[1])) limits.add(Integer.parseInt(value[1]));
        }
        if (limits.isEmpty()) return null;

        return new PotionEffect(effect, Integer.MAX_VALUE, Collections.max(limits), false, false);
    }
}
