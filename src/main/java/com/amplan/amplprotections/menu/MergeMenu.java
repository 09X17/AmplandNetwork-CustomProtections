package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.ItemBuilder;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class MergeMenu implements MenuManager.CustomMenu {

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final int MERGE_BUTTON_SLOT = 49;
    private static final int SELECT_ALL_SLOT = 45;
    private static final int INFO_SLOT = 4;
    private static final int CLOSE_SLOT = 53;

    private final AmplProtections plugin;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<Integer, ProtectionRegion> slotToRegion = new HashMap<>();
    private final Set<Integer> selectedSlots = new HashSet<>();
    private final ProtectionRegion sourceRegion;

    private final String menuTitle;
    private final Material borderMaterial;
    private final String borderName;
    private final int borderCustomModelData;
    private final Material itemMaterial;
    private final int itemCustomModelData;
    private final String itemDisplayName;
    private final List<String> itemLore;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public MergeMenu(AmplProtections plugin, ProtectionRegion sourceRegion) {
        this.plugin = plugin;
        this.sourceRegion = sourceRegion;

        FileConfiguration config = plugin.getMenuConfigManager().getMergeMenu();
        ConfigurationSection menuConfig = config;

        this.menuTitle = menuConfig != null ? menuConfig.getString("title", "<gold><b>FUSIONAR PROTECCIONES</b></gold>")
                : "<gold><b>FUSIONAR PROTECCIONES</b></gold>";
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(menuTitle));

        ConfigurationSection borderCfg = menuConfig != null ? menuConfig.getConfigurationSection("border") : null;
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);

        ConfigurationSection itemCfg = menuConfig != null ? menuConfig.getConfigurationSection("item") : null;
        this.itemMaterial = getMaterial(itemCfg, "material", Material.BEACON);
        this.itemCustomModelData = getCustomModelData(itemCfg, "custom-model-data", -1);
        this.itemDisplayName = getConfigString(itemCfg, "display-name", "<gold><b>%name%</b></gold>");
        this.itemLore = getConfigList(itemCfg, "lore", Arrays.asList(
                "<gray>ID: <yellow>%land_id%",
                "<gray>Centro: <aqua>%x%, %y%, %z%",
                "<gray>Tamano: <yellow>%size%",
                "",
                "<green>Clic para seleccionar"));

        buildInventoryContents();
    }

    private void buildInventoryContents() {
        inventory.clear();
        slotToRegion.clear();
        fillBorder();
        buildAdjacentItems();
        buildInfoItem();
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

    private void buildAdjacentItems() {
        List<ProtectionRegion> adjacent = plugin.getProtectionManager().getAdjacentRegions(sourceRegion);
        int slotIndex = 0;

        for (ProtectionRegion region : adjacent) {
            if (slotIndex >= ITEM_SLOTS.length)
                break;
            if (region.getDatabaseId() == sourceRegion.getDatabaseId())
                continue;
            if (!region.getTypeId().equals(sourceRegion.getTypeId()))
                continue;

            int slot = ITEM_SLOTS[slotIndex];
            int radius = region.getRadius();
            String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);

            boolean isSelected = selectedSlots.contains(slot);

            Material displayMaterial = isSelected ? Material.LIME_DYE : itemMaterial;
            String statusLine = isSelected ? "<green>✓ Seleccionada" : "<gray>Clic para seleccionar";

            List<String> processedLore = new ArrayList<>();
            for (String line : itemLore) {
                processedLore.add(line
                        .replace("%name%", region.getCustomName())
                        .replace("%land_id%", region.getLandId())
                        .replace("%x%", String.valueOf(region.getBlockX()))
                        .replace("%y%", String.valueOf(region.getBlockY()))
                        .replace("%z%", String.valueOf(region.getBlockZ()))
                        .replace("%size%", size));
            }
            processedLore.add("");
            processedLore.add(statusLine);

            ItemStack item = new ItemBuilder(displayMaterial)
                    .setDisplayName(itemDisplayName.replace("%name%", region.getCustomName()))
                    .setLore(processedLore.toArray(String[]::new))
                    .build();

            if (itemCustomModelData != -1) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(itemCustomModelData);
                    item.setItemMeta(meta);
                }
            }

            inventory.setItem(slot, item);
            slotToRegion.put(slot, region);
            slotIndex++;
        }
    }

    private void buildInfoItem() {
        List<ProtectionRegion> selectedRegions = getSelectedRegions();
        selectedRegions.add(0, sourceRegion);

        ProtectionManager.MergeSimulation sim = plugin.getProtectionManager().simulateMerge(selectedRegions);

        String displayName;
        List<String> lore = new ArrayList<>();

        if (selectedRegions.size() < 2) {
            displayName = "<yellow><b>Selecciona protecciones</b></yellow>";
            lore.add("<gray>Selecciona al menos 2 protecciones");
            lore.add("<gray>para ver la vista previa");
        } else if (sim == null) {
            displayName = "<red><b>No se puede fusionar</b></red>";
            lore.add("<red>✘ Error");
        } else if (sim.canMerge()) {
            displayName = "<green><b>Vista previa</b></green>";
            lore.add("<gray>Tamaño final: <yellow>" + sim.getWidth() + "x" + sim.getLength());
            lore.add("<gray>Área: <yellow>" + sim.getArea() + " bloques");
            if (sim.getLargestType() != null) {
                lore.add("<gray>Tipo base: <aqua>" + sim.getLargestType().getDisplayName());
            }
            lore.add("");
            lore.add("<green>✓ Se puede fusionar");
        } else {
            displayName = "<red><b>No se puede fusionar</b></red>";
            lore.add("<gray>Tamaño final: <yellow>" + sim.getWidth() + "x" + sim.getLength());
            lore.add("<gray>Área: <yellow>" + sim.getArea() + " bloques");
            lore.add("");
            lore.add("<red>✘ " + sim.getRejectReason());
        }

        ItemStack infoItem = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .setDisplayName(displayName)
                .setLore(lore.toArray(String[]::new))
                .build();

        inventory.setItem(INFO_SLOT, infoItem);
    }

    private void buildFooter() {
        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .setDisplayName("<red><b>Cerrar</b></red>")
                .setLore("<gray>Clic para salir")
                .build();
        inventory.setItem(CLOSE_SLOT, closeBtn);

        ItemStack selectAllBtn = new ItemBuilder(Material.CHEST)
                .setDisplayName("<aqua><b>Seleccionar Todo</b></aqua>")
                .setLore("<gray>Clic para seleccionar todas")
                .build();
        inventory.setItem(SELECT_ALL_SLOT, selectAllBtn);

        List<ProtectionRegion> selectedRegions = getSelectedRegions();
        selectedRegions.add(0, sourceRegion);
        ProtectionManager.MergeSimulation sim = plugin.getProtectionManager().simulateMerge(selectedRegions);
        boolean canMerge = selectedRegions.size() >= 2 && sim != null && sim.canMerge();

        Material mergeMaterial = canMerge ? Material.LIME_DYE : Material.GRAY_DYE;
        String mergeName = canMerge ? "<green><b>Fusionar</b></green>" : "<gray><b>Fusionar</b></gray>";
        List<String> mergeLore = new ArrayList<>();
        mergeLore.add("<gray>Protecciones: <yellow>" + selectedRegions.size());
        if (canMerge && sim != null) {
            mergeLore.add("<gray>Resultado: <yellow>" + sim.getWidth() + "x" + sim.getLength());
        }
        mergeLore.add("");
        mergeLore.add(canMerge ? "<green>Clic para fusionar" : "<red>Selecciona protecciones válidas");

        ItemStack mergeBtn = new ItemBuilder(mergeMaterial)
                .setDisplayName(mergeName)
                .setLore(mergeLore.toArray(String[]::new))
                .build();
        inventory.setItem(MERGE_BUTTON_SLOT, mergeBtn);
    }

    private List<ProtectionRegion> getSelectedRegions() {
        List<ProtectionRegion> regions = new ArrayList<>();
        for (int slot : selectedSlots) {
            ProtectionRegion region = slotToRegion.get(slot);
            if (region != null) {
                regions.add(region);
            }
        }
        return regions;
    }

    @Override
    public void handleMenuClick(int slot, Player player) {
        if (slot == CLOSE_SLOT) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            return;
        }

        if (slot == SELECT_ALL_SLOT) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            if (selectedSlots.size() == slotToRegion.size()) {
                selectedSlots.clear();
            } else {
                selectedSlots.clear();
                selectedSlots.addAll(slotToRegion.keySet());
            }
            buildInventoryContents();
            player.updateInventory();
            return;
        }

        if (slot == MERGE_BUTTON_SLOT) {
            List<ProtectionRegion> selectedRegions = getSelectedRegions();
            selectedRegions.add(0, sourceRegion);

            if (selectedRegions.size() < 2) {
                player.sendMessage(mm.deserialize("<red>✘ Selecciona al menos 2 protecciones para fusionar."));
                return;
            }

            ProtectionManager.MergeSimulation sim = plugin.getProtectionManager().simulateMerge(selectedRegions);
            if (sim != null && sim.canMerge()) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                player.closeInventory();
                plugin.getProtectionManager().mergeRegions(selectedRegions, player);
            } else {
                player.sendMessage(mm.deserialize(
                        "<red>✘ No se puede fusionar: " + (sim != null ? sim.getRejectReason() : "Error")));
            }
            return;
        }

        if (slotToRegion.containsKey(slot)) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            if (selectedSlots.contains(slot)) {
                selectedSlots.remove(slot);
            } else {
                selectedSlots.add(slot);
            }
            buildInventoryContents();
            player.updateInventory();
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
