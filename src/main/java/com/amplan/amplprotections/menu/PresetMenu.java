package com.amplan.amplprotections.menu;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.FlagPreset;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.ItemBuilder;
import com.amplan.amplprotections.utils.MessageUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PresetMenu implements MenuManager.CustomMenu {

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final AmplProtections plugin;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<Integer, FlagPreset> slotToPreset = new HashMap<>();
    private final ProtectionRegion region;
    private final Player viewer;

    private final String menuTitle;
    private final Material borderMaterial;
    private final String borderName;
    private final int borderCustomModelData;
    private final Material itemMaterial;
    private final int itemCustomModelData;
    private final String itemDisplayName;
    private final List<String> itemLore;
    private final int closeSlot;
    private final Material closeMaterial;
    private final String closeDisplayName;
    private final List<String> closeLore;
    private final int closeCustomModelData;

    public PresetMenu(AmplProtections plugin, ProtectionRegion region, Player viewer) {
        this.plugin = plugin;
        this.region = region;
        this.viewer = viewer;

        FileConfiguration config = plugin.getMenuConfigManager().getPresetMenu();
        ConfigurationSection menuConfig = config;

        this.menuTitle = menuConfig != null ? menuConfig.getString("title", "<gold><b>PRESETS DE FLAGS</b></gold>") : "<gold><b>PRESETS DE FLAGS</b></gold>";
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(menuTitle));

        ConfigurationSection borderCfg = menuConfig != null ? menuConfig.getConfigurationSection("border") : null;
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);

        ConfigurationSection itemCfg = menuConfig != null ? menuConfig.getConfigurationSection("item") : null;
        this.itemMaterial = getMaterial(itemCfg, "material", Material.PAPER);
        this.itemCustomModelData = getCustomModelData(itemCfg, "custom-model-data", -1);
        this.itemDisplayName = getConfigString(itemCfg, "display-name", "<gold><b>%name%</b></gold>");
        this.itemLore = getConfigList(itemCfg, "lore", Arrays.asList(
                "<gray>%description%",
                "",
                "<green>Clic para aplicar preset"
        ));

        ConfigurationSection closeCfg = menuConfig != null ? menuConfig.getConfigurationSection("close-button") : null;
        this.closeSlot = closeCfg != null ? closeCfg.getInt("slot", 49) : 49;
        this.closeMaterial = getMaterial(closeCfg, "material", Material.BARRIER);
        this.closeDisplayName = getConfigString(closeCfg, "display-name", "<red><b>Cerrar</b></red>");
        this.closeLore = getConfigList(closeCfg, "lore", Collections.singletonList("<gray>Clic para salir"));
        this.closeCustomModelData = getCustomModelData(closeCfg, "custom-model-data", -1);

        buildInventoryContents();
    }

    private void buildInventoryContents() {
        inventory.clear();
        slotToPreset.clear();
        fillBorder();
        buildPresetItems();
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
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    private void buildPresetItems() {
        List<FlagPreset> presets = plugin.getPresetManager().getPlayerPresets(viewer);
        int slotIndex = 0;

        for (FlagPreset preset : presets) {
            if (slotIndex >= ITEM_SLOTS.length) break;

            int slot = ITEM_SLOTS[slotIndex];
            String displayName = preset.isGlobal() ? preset.getName().toUpperCase() : preset.getName();
            String description = preset.getDescription() != null ? preset.getDescription() : "";

            List<String> processedLore = new ArrayList<>();
            for (String line : itemLore) {
                processedLore.add(line
                        .replace("%name%", displayName)
                        .replace("%description%", description));
            }

            ItemStack item = new ItemBuilder(itemMaterial)
                    .setDisplayName(itemDisplayName.replace("%name%", displayName))
                    .setLore(processedLore.toArray(new String[0]))
                    .build();

            if (itemCustomModelData != -1) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(itemCustomModelData);
                    item.setItemMeta(meta);
                }
            }

            inventory.setItem(slot, item);
            slotToPreset.put(slot, preset);
            slotIndex++;
        }
    }

    private void buildFooter() {
        ItemStack closeBtn = new ItemBuilder(closeMaterial)
                .setDisplayName(closeDisplayName)
                .setLore(closeLore.toArray(new String[0]))
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
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            return;
        }

        if (slotToPreset.containsKey(slot)) {
            FlagPreset preset = slotToPreset.get(slot);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();

            preset.applyToRegion(region);
            plugin.getProtectionManager().getProtectionDao().saveFlagsAsync(region);

            String msg = MessageUtils.lang("preset.applied").replace("%preset%", preset.getName());
            player.sendMessage(mm.deserialize(msg));
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
        if (section == null) return def;
        String matName = section.getString(path);
        if (matName == null) return def;
        try { return Material.valueOf(matName); } catch (IllegalArgumentException e) { return def; }
    }

    private List<String> getConfigList(ConfigurationSection section, String path, List<String> def) {
        return section != null ? section.getStringList(path) : def;
    }

    private int getCustomModelData(ConfigurationSection section, String path, int def) {
        return section != null ? section.getInt(path, def) : def;
    }
}
