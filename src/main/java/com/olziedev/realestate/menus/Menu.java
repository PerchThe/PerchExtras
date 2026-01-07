package com.olziedev.realestate.menus;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.olziedev.olziemenu.framework.menu.CachedMenu;
import com.olziedev.olziemenu.framework.menu.FrameworkMenu;
import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.estate.EState;
import com.olziedev.realestate.estate.rent.RentingEstate;
import com.olziedev.realestate.managers.DatabaseManager;
import com.olziedev.realestate.managers.Manager;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Menu {

    protected final Manager manager;
    protected CachedMenu cachedMenu;

    public Menu(Manager manager) {
        this.manager = manager;
    }

    public abstract void load();

    protected abstract ConfigurationSection getSection();

    protected DatabaseManager getDatabaseManager() {
        return RealEstate.getDatabaseManager();
    }

    public void kickBed(Player player) { // this is to stop people from opening menus in bed.
        Bukkit.getScheduler().runTask(manager.plugin.plugin, () -> {
            if (!player.isSleeping()) return;

            GameMode currentGameMode = player.getGameMode();
            double currentHealth = player.getHealth();
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20);
            player.damage(1);
            player.setGameMode(currentGameMode);
            player.setHealth(currentHealth);
        });
    }

    public void createItems(ConfigurationSection section, String items, String otherItems) {
        if (section == null) return;

        ConfigurationSection itemSection;
        try {
            if (items != null) {
                itemSection = section.getConfigurationSection(items);
                for (String clickable : itemSection == null ? Collections.<String>emptyList() : itemSection.getKeys(false)) {
                    ItemStack itemStack = this.createItem(section.getConfigurationSection(items + "." + clickable));
                    int slot = section.getInt(items + "." + clickable + ".slot");
                    if (slot == -1) continue;

                    this.cachedMenu.setItem(slot, itemStack);
                }
            }
            if (otherItems != null) {
                itemSection = section.getConfigurationSection(otherItems);
                for (String clickable : itemSection == null ? Collections.<String>emptyList() : itemSection.getKeys(false)) {
                    ItemStack itemStack = this.createItem(section.getConfigurationSection(otherItems + "." + clickable));
                    int slot = section.getInt(otherItems + "." + clickable + ".slot");
                    if (slot == -1) continue;

                    this.cachedMenu.setItem(slot, itemStack);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createItems(FrameworkMenu frameworkMenu, EState eState, ConfigurationSection section, String items, String otherItems) {
        if (section == null) return;

        ConfigurationSection itemSection;
        try {
            if (items != null) {
                itemSection = section.getConfigurationSection(items);
                for (String clickable : itemSection == null ? Collections.<String>emptyList() : itemSection.getKeys(false)) {
                    ItemStack itemStack = this.createItem(section.getConfigurationSection(items + "." + clickable), null, lore -> lore.stream().map(x -> x
                            .replace("%time%", eState instanceof RentingEstate ? Utils.formatTime(((RentingEstate) eState).getTime() / 1000) : "%time%")
                            .replace("%price%", Utils.formatNumber(eState.getPrice()))).collect(Collectors.toList()));
                    int slot = section.getInt(items + "." + clickable + ".slot");
                    if (slot == -1) continue;

                    frameworkMenu.setItem(slot, itemStack);
                }
            }
            if (otherItems != null) {
                itemSection = section.getConfigurationSection(otherItems);
                for (String clickable : itemSection == null ? Collections.<String>emptyList() : itemSection.getKeys(false)) {
                    ItemStack itemStack = this.createItem(section.getConfigurationSection(otherItems + "." + clickable), null, lore -> lore.stream().map(x -> x
                            .replace("%time%", eState instanceof RentingEstate ? Utils.formatTime(((RentingEstate) eState).getTime() / 1000) : "%time%")
                            .replace("%price%", Utils.formatNumber(eState.getPrice()))).collect(Collectors.toList()));
                    int slot = section.getInt(otherItems + "." + clickable + ".slot");
                    if (slot == -1) continue;

                    frameworkMenu.setItem(slot, itemStack);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addItems(FrameworkMenu inventory) {
        for (int i = 0; i < this.cachedMenu.getSize(); i++) {
            ItemStack item = this.cachedMenu.getItem(i);
            if (item == null) continue;

            inventory.setItem(i, item);
        }
    }

    public ItemStack createItem(ConfigurationSection section) {
        return createItem(section, null, null);
    }

    public ItemStack createItem(ConfigurationSection section, Function<String, String> nameReplacements, Function<List<String>, List<String>> loreReplacements) {
        if (section == null) return null;

        Material material = Material.getMaterial(Configuration.getString(section, "material").toUpperCase());
        if (material == null || material == Material.AIR) return null;
        if (material == getSkullMaterial() && (section.getString("owner") != null || section.getString("texture") != null)) return createSkull(section, nameReplacements, loreReplacements);

        ItemStack itemStack = new ItemStack(material, section.getInt("amount", 1), (short) section.getInt("data"));
        return addBaseItemMeta(itemStack, itemStack.getItemMeta(), section, nameReplacements, loreReplacements);
    }

    public ItemStack createSkull(ConfigurationSection section, Function<String, String> nameReplacements, Function<List<String>, List<String>> loreReplacements) {
        if (section == null) return null;

        ItemStack itemStack = new ItemStack(getSkullMaterial(), section.getInt("amount", 1), (short) section.getInt("data"));
        return addBaseItemMeta(itemStack, applyBasicSkull(itemStack, section), section, nameReplacements, loreReplacements);
    }

    public SkullMeta applyBasicSkull(ItemStack itemStack, ConfigurationSection section) {
        SkullMeta im = (SkullMeta) itemStack.getItemMeta();
        String owner = Configuration.getString(section, "owner");
        if (!owner.isEmpty()) im.setOwner(owner);

        String texture = Configuration.getString(section, "texture");
        if (!texture.isEmpty()) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));

            try {
                Field profileField = im.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(im, profile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return im;
    }

    private ItemStack addBaseItemMeta(ItemStack itemStack, ItemMeta im, ConfigurationSection section, Function<String, String> nameReplacements, Function<List<String>, List<String>> loreReplacements) {
        if (section == null || itemStack.getAmount() <= 0) return null;

        String name = Utils.color(section.getString("name"));
        im.setDisplayName((nameReplacements == null ? name : nameReplacements.apply(name)));

        List<String> lore = section.getStringList("lore");
        im.setLore((loreReplacements == null ? lore : loreReplacements.apply(lore)).stream().map(Utils::color).collect(Collectors.toList()));

        if (section.getBoolean("glowing")) {
            im.addEnchant(Enchantment.DURABILITY, 1, true);
            try {
                im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } catch (NoClassDefFoundError ignored) {}
        }
        try {
            section.getStringList("item-flags").forEach(x -> im.addItemFlags(ItemFlag.valueOf(x)));
        } catch (NoClassDefFoundError ignored) {}
        try {
            section.getStringList("enchantments").forEach(x -> im.addEnchant(Enchantment.getByName(x.split(":")[0].toUpperCase()), Integer.parseInt(x.split(":")[1]), true));
        } catch (NoClassDefFoundError ignored) {}
        try {
            int customModelData = section.getInt("custom-model-data", -1);
            if (customModelData != -1) im.setCustomModelData(customModelData);
        } catch (Throwable ignored) {}

        itemStack.setItemMeta(im);

        return itemStack;
    }

    public Material getSkullMaterial() {
        Material icon = Material.getMaterial("PLAYER_HEAD");
        if (icon == null) return Material.getMaterial("SKULL_ITEM");
        return icon;
    }

    public abstract void open(Player player);
}
