package com.olziedev.bulkmapart;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;

final class BulkMapartCommand implements CommandExecutor, TabCompleter {

    private static final String USE_PERMISSION = "evergreen.bulkmapart.use";
    private static final String RELOAD_PERMISSION = "evergreen.bulkmapart.reload";
    private static final NamespacedKey COPYRIGHT_OWNER_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("perchcopyright:owner")
    );

    private final JavaPlugin plugin;
    private final Messages messages;

    BulkMapartCommand(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadMessages(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            messages.send(sender, "only-players");
            return true;
        }

        if (!player.hasPermission(USE_PERMISSION)) {
            messages.send(player, "no-permission");
            return true;
        }

        if (args.length != 1) {
            messages.send(
                    player,
                    "usage",
                    Messages.value("label", label),
                    Messages.value("argument", "<amount>")
            );
            return true;
        }

        Integer amount = parsePositiveAmount(args[0]);
        if (amount == null) {
            messages.send(player, "invalid-amount");
            return true;
        }

        TemplateResult result = inspectTemplate(
                player.getInventory().getItemInMainHand(),
                player.getUniqueId()
        );
        if (result.errorKey() != null) {
            if (result.containerName() == null) {
                messages.send(player, result.errorKey());
            } else {
                messages.send(
                        player,
                        result.errorKey(),
                        Messages.value("container", result.containerName())
                );
            }
            return true;
        }

        duplicate(player, result.template(), amount);
        return true;
    }

    private void duplicate(Player player, Template template, int amount) {
        PlayerInventory inventory = player.getInventory();
        long requiredMaps = (long) template.mapsPerCopy() * amount;
        long requiredItemFrames = (long) template.itemFramesPerCopy() * amount;
        long requiredGlowItemFrames = (long) template.glowItemFramesPerCopy() * amount;
        long availableContainers = countItems(
                inventory.getStorageContents(),
                item -> isUsableEmptyContainer(item, template.kind(), template.material())
        );
        long availableMaps = countItems(
                inventory.getStorageContents(),
                BulkMapartCommand::isUsableEmptyMap
        );
        long availableItemFrames = countItems(
                inventory.getStorageContents(),
                item -> item.getType() == Material.ITEM_FRAME
        );
        long availableGlowItemFrames = countItems(
                inventory.getStorageContents(),
                item -> item.getType() == Material.GLOW_ITEM_FRAME
        );

        List<String> missing = new ArrayList<>(4);
        if (availableContainers < amount) {
            long missingContainers = amount - availableContainers;
            missing.add(formatCount(missingContainers, materialName(template.material())));
        }
        if (availableMaps < requiredMaps) {
            missing.add(formatCount(requiredMaps - availableMaps, "empty map"));
        }
        if (availableItemFrames < requiredItemFrames) {
            missing.add(formatCount(requiredItemFrames - availableItemFrames, "item frame"));
        }
        if (availableGlowItemFrames < requiredGlowItemFrames) {
            missing.add(formatCount(requiredGlowItemFrames - availableGlowItemFrames, "glow item frame"));
        }

        if (!missing.isEmpty()) {
            messages.send(
                    player,
                    "missing-items",
                    Messages.component("items", formatMissingList(missing))
            );
            return;
        }

        ItemStack[] original = cloneContents(inventory.getStorageContents());
        ItemStack[] working = cloneContents(original);
        takeItems(
                working,
                item -> isUsableEmptyContainer(item, template.kind(), template.material()),
                amount
        );
        takeItems(working, BulkMapartCommand::isUsableEmptyMap, requiredMaps);
        takeItems(working, item -> item.getType() == Material.ITEM_FRAME, requiredItemFrames);
        takeItems(working, item -> item.getType() == Material.GLOW_ITEM_FRAME, requiredGlowItemFrames);

        try {
            inventory.setStorageContents(working);
            for (int i = 0; i < amount; i++) {
                ItemStack duplicate = template.item().clone();
                duplicate.setAmount(1);
                Map<Integer, ItemStack> leftovers = inventory.addItem(duplicate);
                if (!leftovers.isEmpty()) {
                    inventory.setStorageContents(original);
                    messages.send(
                            player,
                            "not-enough-space",
                            Messages.value("amount", amount),
                            Messages.value("duplicate_word", amount == 1 ? "duplicate" : "duplicates")
                    );
                    return;
                }
            }
        } catch (RuntimeException exception) {
            inventory.setStorageContents(original);
            throw exception;
        }

        if (requiredItemFrames == 0 && requiredGlowItemFrames == 0) {
            messages.send(
                    player,
                    "success",
                    Messages.value("amount", amount),
                    Messages.value("copy_word", amount == 1 ? "copy" : "copies"),
                    Messages.value("containers", formatCount(amount, materialName(template.material()))),
                    Messages.value("maps", formatCount(requiredMaps, "empty map"))
            );
        } else {
            List<String> materials = new ArrayList<>(4);
            materials.add(formatCount(amount, materialName(template.material())));
            materials.add(formatCount(requiredMaps, "empty map"));
            if (requiredItemFrames > 0) {
                materials.add(formatCount(requiredItemFrames, "item frame"));
            }
            if (requiredGlowItemFrames > 0) {
                materials.add(formatCount(requiredGlowItemFrames, "glow item frame"));
            }
            messages.send(
                    player,
                    "success-with-frames",
                    Messages.value("amount", amount),
                    Messages.value("copy_word", amount == 1 ? "copy" : "copies"),
                    Messages.value("materials", joinWithAnd(materials))
            );
        }
    }

    private void reloadMessages(CommandSender sender) {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            messages.send(sender, "no-permission");
            return;
        }

        try {
            messages.reload();
            messages.send(sender, "reload-success");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not reload bulkmapart/messages.yml.", exception);
            messages.send(sender, "reload-failure");
        }
    }

    private static TemplateResult inspectTemplate(ItemStack held, UUID playerId) {
        ContainerKind kind = containerKind(held.getType());
        if (kind == null) {
            return TemplateResult.error("hold-container");
        }

        List<ItemStack> contents;
        if (kind == ContainerKind.SHULKER_BOX) {
            if (!(held.getItemMeta() instanceof BlockStateMeta meta)) {
                return TemplateResult.error("unreadable-shulker");
            }
            BlockState state = meta.getBlockState();
            if (!(state instanceof ShulkerBox shulkerBox)) {
                return TemplateResult.error("unreadable-shulker");
            }
            contents = Arrays.asList(shulkerBox.getInventory().getContents());
        } else {
            if (!(held.getItemMeta() instanceof BundleMeta meta)) {
                return TemplateResult.error("unreadable-bundle");
            }
            contents = meta.getItems();
        }

        long mapCount = 0;
        long itemFrameCount = 0;
        long glowItemFrameCount = 0;
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (item.getType() == Material.FILLED_MAP) {
                if (!isOwnedByOrUnclaimed(item, playerId)) {
                    return TemplateResult.error("not-map-owner");
                }
                mapCount += item.getAmount();
            } else if (item.getType() == Material.ITEM_FRAME) {
                itemFrameCount += item.getAmount();
            } else if (item.getType() == Material.GLOW_ITEM_FRAME) {
                glowItemFrameCount += item.getAmount();
            } else {
                return TemplateResult.error("only-mapart-items", materialName(held.getType()));
            }
        }

        if (mapCount == 0) {
            return TemplateResult.error("no-filled-maps", materialName(held.getType()));
        }
        if (mapCount > Integer.MAX_VALUE
                || itemFrameCount > Integer.MAX_VALUE
                || glowItemFrameCount > Integer.MAX_VALUE) {
            return TemplateResult.error("too-many-items");
        }

        ItemStack templateItem = held.clone();
        templateItem.setAmount(1);
        return TemplateResult.success(new Template(
                kind,
                held.getType(),
                (int) mapCount,
                (int) itemFrameCount,
                (int) glowItemFrameCount,
                templateItem
        ));
    }

    private static boolean isOwnedByOrUnclaimed(ItemStack map, UUID playerId) {
        String owner = map.getItemMeta()
                .getPersistentDataContainer()
                .get(COPYRIGHT_OWNER_KEY, PersistentDataType.STRING);
        if (owner == null) {
            return true;
        }

        try {
            return UUID.fromString(owner).equals(playerId);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isUsableEmptyMap(ItemStack item) {
        if (item.getType() != Material.MAP) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return !meta.hasCustomName() && !meta.hasDisplayName() && !meta.hasItemName();
    }

    private static boolean isUsableEmptyContainer(
            ItemStack item,
            ContainerKind expectedKind,
            Material expectedMaterial
    ) {
        if (item.getType() != expectedMaterial || containerKind(item.getType()) != expectedKind) {
            return false;
        }

        if (expectedKind == ContainerKind.BUNDLE) {
            return item.getItemMeta() instanceof BundleMeta meta && meta.getItems().isEmpty();
        }

        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) {
            return false;
        }
        return meta.getBlockState() instanceof ShulkerBox shulkerBox
                && shulkerBox.getInventory().isEmpty();
    }

    private static @Nullable ContainerKind containerKind(Material material) {
        String name = material.name();
        if (name.equals("SHULKER_BOX") || name.endsWith("_SHULKER_BOX")) {
            return ContainerKind.SHULKER_BOX;
        }
        if (name.equals("BUNDLE") || name.endsWith("_BUNDLE")) {
            return ContainerKind.BUNDLE;
        }
        return null;
    }

    private static long countItems(ItemStack[] contents, Predicate<ItemStack> predicate) {
        long count = 0;
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir() && predicate.test(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static void takeItems(ItemStack[] contents, Predicate<ItemStack> predicate, long amount) {
        long remaining = amount;
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir() || !predicate.test(item)) {
                continue;
            }

            int taken = (int) Math.min(remaining, item.getAmount());
            if (taken == item.getAmount()) {
                contents[slot] = null;
            } else {
                item.setAmount(item.getAmount() - taken);
            }
            remaining -= taken;
        }

        if (remaining != 0) {
            throw new IllegalStateException("Inventory changed while preparing the bulk map-art operation");
        }
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] cloned = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            cloned[i] = contents[i] == null ? null : contents[i].clone();
        }
        return cloned;
    }

    private static @Nullable Integer parsePositiveAmount(String input) {
        try {
            int amount = Integer.parseInt(input);
            return amount > 0 ? amount : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String materialName(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String formatCount(long amount, String singularName) {
        return amount + " " + pluralize(singularName, amount);
    }

    private static String pluralize(String singularName, long amount) {
        if (amount == 1) {
            return singularName;
        }
        if (singularName.endsWith("s")
                || singularName.endsWith("x")
                || singularName.endsWith("z")
                || singularName.endsWith("ch")
                || singularName.endsWith("sh")) {
            return singularName + "es";
        }
        if (singularName.endsWith("y") && singularName.length() > 1) {
            char beforeY = singularName.charAt(singularName.length() - 2);
            if ("aeiou".indexOf(beforeY) < 0) {
                return singularName.substring(0, singularName.length() - 1) + "ies";
            }
        }
        return singularName + "s";
    }

    private Component formatMissingList(List<String> items) {
        Component list = Component.empty();
        for (int index = 0; index < items.size(); index++) {
            if (index > 0) {
                list = list.append(Component.newline());
            }
            list = list.append(messages.render(
                    "missing-list-item",
                    Messages.value("item", items.get(index)),
                    Messages.value("punctuation", index == items.size() - 1 ? "." : ",")
            ));
        }
        return list;
    }

    private static String joinWithAnd(List<String> items) {
        if (items.size() == 1) {
            return items.getFirst();
        }
        if (items.size() == 2) {
            return items.get(0) + " and " + items.get(1);
        }
        return String.join(", ", items.subList(0, items.size() - 1))
                + ", and " + items.getLast();
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            if (sender.hasPermission(USE_PERMISSION)) {
                suggestions.addAll(List.of("1", "2", "4", "8"));
            }
            if (sender.hasPermission(RELOAD_PERMISSION)) {
                suggestions.add("reload");
            }
            return suggestions.stream()
                    .filter(suggestion -> suggestion.startsWith(args[0]))
                    .toList();
        }
        return List.of();
    }

    private enum ContainerKind {
        SHULKER_BOX,
        BUNDLE
    }

    private record Template(
            ContainerKind kind,
            Material material,
            int mapsPerCopy,
            int itemFramesPerCopy,
            int glowItemFramesPerCopy,
            ItemStack item
    ) {
    }

    private record TemplateResult(
            @Nullable Template template,
            @Nullable String errorKey,
            @Nullable String containerName
    ) {

        private static TemplateResult success(Template template) {
            return new TemplateResult(template, null, null);
        }

        private static TemplateResult error(String errorKey) {
            return new TemplateResult(null, errorKey, null);
        }

        private static TemplateResult error(String errorKey, String containerName) {
            return new TemplateResult(null, errorKey, containerName);
        }
    }
}
