package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.ProtectionType;
import com.amplan.amplprotections.utils.ItemBuilder;
import com.amplan.amplprotections.utils.SkullUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class BuyMenu implements MenuManager.CustomMenu {

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final AmplProtections plugin;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<Integer, String> slotToTypeId = new HashMap<>();

    private final String menuTitle;
    private final Material borderMaterial;
    private final String borderName;
    private final int borderCustomModelData;
    private final Material itemMaterial;
    private final int itemCustomModelData;
    private final String itemSkullValue;
    private final String itemDisplayName;
    private final List<String> itemLore;
    private final int backSlot;
    private final Material backMaterial;
    private final String backDisplayName;
    private final List<String> backLore;
    private final int backCustomModelData;
    private final int closeSlot;
    private final Material closeMaterial;
    private final String closeDisplayName;
    private final List<String> closeLore;
    private final int closeCustomModelData;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public BuyMenu(AmplProtections plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getMenuConfigManager().getBuyMenu();
        ConfigurationSection menuConfig = config;

        this.menuTitle = menuConfig != null
                ? menuConfig.getString("title", "<gold><b>TIENDA DE PROTECCIONES</b></gold>")
                : "<gold><b>TIENDA DE PROTECCIONES</b></gold>";
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(menuTitle));

        ConfigurationSection borderCfg = menuConfig != null ? menuConfig.getConfigurationSection("border") : null;
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);

        ConfigurationSection itemCfg = menuConfig != null ? menuConfig.getConfigurationSection("item") : null;
        this.itemMaterial = getMaterial(itemCfg, "material", Material.PLAYER_HEAD);
        this.itemCustomModelData = getCustomModelData(itemCfg, "custom-model-data", -1);
        this.itemSkullValue = getConfigString(itemCfg, "skull-value", null);
        this.itemDisplayName = getConfigString(itemCfg, "display-name", "<gold><b>%name%</b></gold>");
        this.itemLore = getConfigList(itemCfg, "lore", Arrays.asList(
                "<gray>Tamano: <yellow>%size%x%size%",
                "<gray>Radio: <aqua>%radius% bloques",
                "",
                "<green>Precio: <yellow>$%price%",
                "",
                "<gray>Clic para comprar"));

        ConfigurationSection backCfg = menuConfig != null ? menuConfig.getConfigurationSection("back-button") : null;
        this.backSlot = backCfg != null ? backCfg.getInt("slot", 49) : 49;
        this.backMaterial = getMaterial(backCfg, "material", Material.ARROW);
        this.backDisplayName = getConfigString(backCfg, "display-name", "<aqua><b>Volver</b></aqua>");
        this.backLore = getConfigList(backCfg, "lore", Collections.singletonList("<gray>Clic para volver"));
        this.backCustomModelData = getCustomModelData(backCfg, "custom-model-data", -1);

        ConfigurationSection closeCfg = menuConfig != null ? menuConfig.getConfigurationSection("close-button") : null;
        this.closeSlot = closeCfg != null ? closeCfg.getInt("slot", 53) : 53;
        this.closeMaterial = getMaterial(closeCfg, "material", Material.BARRIER);
        this.closeDisplayName = getConfigString(closeCfg, "display-name", "<red><b>Cerrar</b></red>");
        this.closeLore = getConfigList(closeCfg, "lore", Collections.singletonList("<gray>Clic para salir"));
        this.closeCustomModelData = getCustomModelData(closeCfg, "custom-model-data", -1);

        buildInventoryContents();
    }

    private void buildInventoryContents() {
        inventory.clear();
        slotToTypeId.clear();
        fillBorder();
        buildProtectionItems();
        buildFooter();
    }

    private void fillBorder() {
        ItemStack border = new ItemBuilder(borderMaterial)
                .setDisplayName(borderName)
                .build();
        if (borderCustomModelData != -1) {
            ItemMeta meta = border.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(borderCustomModelData);
                border.setItemMeta(meta);
            }
        }
        for (int i = 0; i < 9; i++)
            inventory.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, border);
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    private void buildProtectionItems() {
        Map<String, ProtectionType> types = plugin.getProtectionManager().getProtectionTypes();
        int slotIndex = 0;

        for (Map.Entry<String, ProtectionType> entry : types.entrySet()) {
            if (slotIndex >= ITEM_SLOTS.length)
                break;

            String typeId = entry.getKey();
            ProtectionType type = entry.getValue();
            int slot = ITEM_SLOTS[slotIndex];

            double price = plugin.getEconomyManager().getCost(typeId);
            int radius = type.getRadius();
            String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);

            List<String> processedLore = new ArrayList<>();
            for (String line : itemLore) {
                processedLore.add(line
                        .replace("%name%", type.getDisplayName())
                        .replace("%size%", size)
                        .replace("%radius%", String.valueOf(radius))
                        .replace("%price%",
                                plugin.getEconomyManager().isEnabled() ? plugin.getEconomyManager().format(price)
                                        : "Gratis")
                        .replace("%type%", typeId));
            }

            ItemStack item;
            if (itemMaterial == Material.PLAYER_HEAD && itemSkullValue != null && !itemSkullValue.isEmpty()) {
                item = SkullUtils.createSkullWithTexture(itemSkullValue);
            } else if (itemMaterial == Material.PLAYER_HEAD && type.getMaterial() == Material.PLAYER_HEAD
                    && type.getSkullValue() != null && !type.getSkullValue().isEmpty()) {
                item = SkullUtils.createSkullWithTexture(type.getSkullValue());
            } else {
                item = new ItemStack(type.getMaterial());
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(mm.deserialize(itemDisplayName.replace("%name%", type.getDisplayName())));
                List<net.kyori.adventure.text.Component> loreComponents = processedLore.stream()
                        .map(mm::deserialize)
                        .toList();
                meta.lore(loreComponents);
                if (itemCustomModelData != -1)
                    meta.setCustomModelData(itemCustomModelData);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
            slotToTypeId.put(slot, typeId);
            slotIndex++;
        }
    }

    private void buildFooter() {
        ItemStack backBtn = new ItemBuilder(backMaterial)
                .setDisplayName(backDisplayName)
                .setLore(backLore.toArray(String[]::new))
                .build();
        if (backCustomModelData != -1) {
            ItemMeta meta = backBtn.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(backCustomModelData);
                backBtn.setItemMeta(meta);
            }
        }
        inventory.setItem(backSlot, backBtn);

        ItemStack closeBtn = new ItemBuilder(closeMaterial)
                .setDisplayName(closeDisplayName)
                .setLore(closeLore.toArray(String[]::new))
                .build();
        if (closeCustomModelData != -1) {
            ItemMeta meta = closeBtn.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(closeCustomModelData);
                closeBtn.setItemMeta(meta);
            }
        }
        inventory.setItem(closeSlot, closeBtn);
    }

    @Override
    public void handleMenuClick(int slot, Player player) {
        if (slot == closeSlot) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            return;
        }

        if (slot == backSlot) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            return;
        }

        if (slotToTypeId.containsKey(slot)) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            String typeId = slotToTypeId.get(slot);
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();

            plugin.getProtectionManager().createRegionFromBuy(player, typeId);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private String getConfigString(ConfigurationSection section, String path, String def) {
        return section != null ? section.getString(path, def) : def;
    }

    private Material getMaterial(ConfigurationSection section, String path, Material def) {
        if (section == null)
            return def;
        String matName = section.getString(path);
        if (matName == null)
            return def;
        try {
            return Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private List<String> getConfigList(ConfigurationSection section, String path, List<String> def) {
        return section != null ? section.getStringList(path) : def;
    }

    private int getCustomModelData(ConfigurationSection section, String path, int def) {
        return section != null ? section.getInt(path, def) : def;
    }
}
