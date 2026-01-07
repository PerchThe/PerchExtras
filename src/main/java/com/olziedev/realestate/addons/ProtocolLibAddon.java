package com.olziedev.realestate.addons;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.player.GUIPlayer;
import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProtocolLibAddon extends Addon {

    private static Map<UUID, SignEditor> inputReceivers;

    public ProtocolLibAddon(RealEstate plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        inputReceivers = new HashMap<>();
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this.plugin.plugin, PacketType.Play.Client.UPDATE_SIGN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) return; // apparently player can be null now wtf?

                SignEditor menu = inputReceivers.remove(player.getUniqueId());
                if (menu == null) return;

                event.setCancelled(true);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success;
                    try {
                        success = menu.response.test(event.getPacket().getStringArrays().read(0));
                    } catch (FieldAccessException ex) {
                        success = menu.response.test(Arrays.stream(event.getPacket().getChatComponentArrays().read(0)).map(x -> TextComponent.toLegacyText(ComponentSerializer.parse(x.getJson()))).toArray(String[]::new));
                    }
                    if (!success) Bukkit.getScheduler().runTaskLater(plugin, () -> menu.open(player, menu.fixFlashing), 2L);

                    PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.BLOCK_CHANGE);

                    packet.getBlockPositionModifier().write(0, menu.position);
                    packet.getBlockData().write(0, WrappedBlockData.createData(menu.block.getType()));
                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                    } catch (InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });
    }

    public void openAmountSign(ConfigurationSection section, GUIPlayer guiPlayer, Predicate<AtomicInteger> predicate, Runnable runnable) {
        Player player = guiPlayer.getPlayer();
        try {
            List<String> list = section.getStringList("lines").stream().map(x -> x.replace("%price%", Utils.formatNumber(guiPlayer.getEstate().getPrice()))).collect(Collectors.toList());
            int index = list.indexOf("%search%");
            ProtocolLibAddon.newSignEditor(list, index, player, lines -> {
                try {
                    AtomicInteger amount = new AtomicInteger(Integer.parseInt(lines[index]));
                    if (predicate.test(amount)) {
                        guiPlayer.setAmount(amount.get());
                    }
                } catch (Throwable ex) {
                    Utils.sendMessage(player, Configuration.getString(Configuration.getConfig(), "lang.not-a-number"));
                }
                runnable.run();
                return true;
            }, section.getBoolean("fix-old-flashing"));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static void newSignEditor(List<String> list, int index, Player player, Predicate<String[]> lines, boolean fixFlashing) {
        list.set(index, "");

        ProtocolLibAddon protocolLibAddon = new ProtocolLibAddon(RealEstate.getInstance());
        protocolLibAddon.newSignEditor(list).response(lines).open(player, fixFlashing);
    }

    public SignEditor newSignEditor(List<String> text) {
        return new SignEditor(text);
    }

    public static class SignEditor {

        private final List<String> text;
        private Predicate<String[]> response;
        private BlockPosition position;
        public Block block;
        public boolean fixFlashing;

        SignEditor(List<String> text) {
            this.text = text;
        }

        public SignEditor response(Predicate<String[]> response) {
            this.response = response;
            return this;
        }

        public void open(Player player, boolean fixFlashing) {
            Location location = player.getLocation();
            this.position = new BlockPosition(location.getBlockX(), fixFlashing ? player.getLocation().getBlockY() : player.getWorld().getMaxHeight() - 1, location.getBlockZ());
            Location blockLocation = this.position.toLocation(player.getWorld());
            this.block = blockLocation.getBlock();
            this.fixFlashing = fixFlashing;

            PacketContainer openSign = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
            try {
                while (text.size() < 4) text.add("");

                this.blockChange(player, blockLocation, null);
                player.sendSignChange(blockLocation, text.stream().map(Utils::color).toArray(String[]::new));
                openSign.getBlockPositionModifier().write(0, this.position);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, openSign);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            inputReceivers.put(player.getUniqueId(), this);
        }

        @SuppressWarnings("deprecation")
        private void blockChange(Player player, Location location, Block block) {
            Material material = Material.getMaterial("OAK_SIGN");
            if (material == null) material = Material.getMaterial("SIGN_POST");

            try {
                player.sendBlockChange(location, block == null ? material.createBlockData() : block.getBlockData());
            } catch (Throwable ignored) {
                player.sendBlockChange(location, block == null ? material : block.getType(), block == null ? (byte) 0 : block.getData());
            }
        }

        private Material getMaterial() {
            Material material = Material.getMaterial("OAK_SIGN");
            if (material == null) return Material.getMaterial("SIGN_POST");
            return material;
        }
    }
}
