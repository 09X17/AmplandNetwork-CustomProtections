package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.ItemBuilder;
import com.amplan.amplprotections.utils.MessageUtils;
import com.amplan.amplprotections.utils.SkullUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class BulkManageMenu implements MenuManager.CustomMenu {

    private static final int ITEMS_PER_PAGE = 21;
    private static final int[] VALID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final AmplProtections plugin;
    private final ProtectionManager manager;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<Integer, ProtectionRegion> slotToRegionMap = new HashMap<>();
    private final int page;
    private final Player ownerPlayer;
    private final Set<Integer> selectedRegionIds = new HashSet<>();

    private final String menuTitle;
    private final Material borderMaterial;
    private final String borderName;
    private final int borderCustomModelData;
    private final Material statsMaterial;
    private final String statsDisplayName;
    private final List<String> statsLore;
    private final int statsCustomModelData;
    private final int closeSlot;
    private final Material closeMaterial;
    private final String closeDisplayName;
    private final List<String> closeLore;
    private final int closeCustomModelData;
    private final int prevSlot;
    private final Material prevMaterial;
    private final String prevDisplayName;
    private final List<String> prevLore;
    private final int prevCustomModelData;
    private final int nextSlot;
    private final Material nextMaterial;
    private final String nextDisplayName;
    private final List<String> nextLore;
    private final int nextCustomModelData;
    private final int pageInfoSlot;
    private final Material pageInfoMaterial;
    private final String pageInfoDisplayName;
    private final List<String> pageInfoLore;
    private final int pageInfoCustomModelData;
    private final int confirmSlot;
    private final Material confirmMaterial;
    private final String confirmDisplayName;
    private final List<String> confirmLore;
    private final int confirmCustomModelData;
    private final int selectAllSlot;
    private final Material selectAllMaterial;
    private final String selectAllDisplayName;
    private final List<String> selectAllLore;
    private final int selectAllCustomModelData;

    private final String regionItemDisplayName;
    private final List<String> regionItemLore;
    private final int regionItemCustomModelData;

    public BulkManageMenu(AmplProtections plugin, Player ownerPlayer) {
        this(plugin, ownerPlayer, 1, new HashSet<>());
    }

    public BulkManageMenu(AmplProtections plugin, Player ownerPlayer, int page, Set<Integer> selectedRegionIds) {
        this.plugin = plugin;
        this.manager = plugin.getProtectionManager();
        this.page = Math.max(1, page);
        this.ownerPlayer = ownerPlayer;
        this.selectedRegionIds.addAll(selectedRegionIds);

        org.bukkit.configuration.file.FileConfiguration menuConfig = plugin.getMenuConfigManager().getMenuConfig("bulk-manage-menu");

        this.menuTitle = menuConfig != null ? menuConfig.getString("title", MessageUtils.lang("bulk-menu.title")) : MessageUtils.lang("bulk-menu.title");
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(menuTitle));

        ConfigurationSection borderCfg = menuConfig != null ? menuConfig.getConfigurationSection("border") : null;
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);

        ConfigurationSection statsCfg = menuConfig != null ? menuConfig.getConfigurationSection("stats-item") : null;
        this.statsMaterial = getMaterial(statsCfg, "material", Material.BEACON);
        this.statsDisplayName = getConfigString(statsCfg, "display-name", MessageUtils.lang("bulk-menu.stats-btn"));
        this.statsLore = getConfigList(statsCfg, "lore", Arrays.asList(
                MessageUtils.lang("bulk-menu.stats-lore-total"),
                MessageUtils.lang("bulk-menu.stats-lore-selected"),
                "",
                MessageUtils.lang("bulk-menu.stats-lore-instruction")
        ));
        this.statsCustomModelData = getCustomModelData(statsCfg, "custom-model-data", -1);

        ConfigurationSection closeCfg = menuConfig != null ? menuConfig.getConfigurationSection("close-button") : null;
        this.closeSlot = closeCfg != null ? closeCfg.getInt("slot", 49) : 49;
        this.closeMaterial = getMaterial(closeCfg, "material", Material.BARRIER);
        this.closeDisplayName = getConfigString(closeCfg, "display-name", MessageUtils.lang("menu.close-btn"));
        this.closeLore = getConfigList(closeCfg, "lore", Collections.singletonList(MessageUtils.lang("menu.close-lore")));
        this.closeCustomModelData = getCustomModelData(closeCfg, "custom-model-data", -1);

        ConfigurationSection prevCfg = menuConfig != null ? menuConfig.getConfigurationSection("prev-button") : null;
        this.prevSlot = prevCfg != null ? prevCfg.getInt("slot", 45) : 45;
        this.prevMaterial = getMaterial(prevCfg, "material", Material.ARROW);
        this.prevDisplayName = getConfigString(prevCfg, "display-name", MessageUtils.lang("bulk-menu.prev-btn"));
        this.prevLore = getConfigList(prevCfg, "lore", Collections.singletonList(MessageUtils.lang("bulk-menu.prev-lore")));
        this.prevCustomModelData = getCustomModelData(prevCfg, "custom-model-data", -1);

        ConfigurationSection nextCfg = menuConfig != null ? menuConfig.getConfigurationSection("next-button") : null;
        this.nextSlot = nextCfg != null ? nextCfg.getInt("slot", 53) : 53;
        this.nextMaterial = getMaterial(nextCfg, "material", Material.ARROW);
        this.nextDisplayName = getConfigString(nextCfg, "display-name", MessageUtils.lang("bulk-menu.next-btn"));
        this.nextLore = getConfigList(nextCfg, "lore", Collections.singletonList(MessageUtils.lang("bulk-menu.next-lore")));
        this.nextCustomModelData = getCustomModelData(nextCfg, "custom-model-data", -1);

        ConfigurationSection pageCfg = menuConfig != null ? menuConfig.getConfigurationSection("page-info") : null;
        this.pageInfoSlot = pageCfg != null ? pageCfg.getInt("slot", 47) : 47;
        this.pageInfoMaterial = getMaterial(pageCfg, "material", Material.KNOWLEDGE_BOOK);
        this.pageInfoDisplayName = getConfigString(pageCfg, "display-name", MessageUtils.lang("bulk-menu.page-info"));
        this.pageInfoLore = getConfigList(pageCfg, "lore", Arrays.asList(
                MessageUtils.lang("bulk-menu.page-lore")
        ));
        this.pageInfoCustomModelData = getCustomModelData(pageCfg, "custom-model-data", -1);

        ConfigurationSection confirmCfg = menuConfig != null ? menuConfig.getConfigurationSection("confirm-button") : null;
        this.confirmSlot = confirmCfg != null ? confirmCfg.getInt("slot", 50) : 50;
        this.confirmMaterial = getMaterial(confirmCfg, "material", Material.EMERALD);
        this.confirmDisplayName = getConfigString(confirmCfg, "display-name", MessageUtils.lang("bulk-menu.confirm-btn"));
        this.confirmLore = getConfigList(confirmCfg, "lore", Arrays.asList(
                MessageUtils.lang("bulk-menu.confirm-lore-1"),
                MessageUtils.lang("bulk-menu.confirm-lore-2")
        ));
        this.confirmCustomModelData = getCustomModelData(confirmCfg, "custom-model-data", -1);

        ConfigurationSection selectAllCfg = menuConfig != null ? menuConfig.getConfigurationSection("select-all-button") : null;
        this.selectAllSlot = selectAllCfg != null ? selectAllCfg.getInt("slot", 51) : 51;
        this.selectAllMaterial = getMaterial(selectAllCfg, "material", Material.GOLDEN_APPLE);
        this.selectAllDisplayName = getConfigString(selectAllCfg, "display-name", MessageUtils.lang("bulk-menu.select-all-btn"));
        this.selectAllLore = getConfigList(selectAllCfg, "lore", Collections.singletonList(MessageUtils.lang("bulk-menu.select-all-lore")));
        this.selectAllCustomModelData = getCustomModelData(selectAllCfg, "custom-model-data", -1);

        ConfigurationSection regionCfg = menuConfig != null ? menuConfig.getConfigurationSection("region-item") : null;
        this.regionItemDisplayName = getConfigString(regionCfg, "display-name", MessageUtils.lang("bulk-menu.region-item-name"));
        this.regionItemLore = getConfigList(regionCfg, "lore", Arrays.asList(
                MessageUtils.lang("bulk-menu.region-lore-id"),
                MessageUtils.lang("bulk-menu.region-lore-world"),
                MessageUtils.lang("bulk-menu.region-lore-center"),
                MessageUtils.lang("bulk-menu.region-lore-size"),
                MessageUtils.lang("bulk-menu.region-lore-members"),
                "",
                MessageUtils.lang("bulk-menu.region-lore-click")
        ));
        this.regionItemCustomModelData = getCustomModelData(regionCfg, "custom-model-data", -1);

        buildInventoryContents();
    }

    private void buildInventoryContents() {
        inventory.clear();
        slotToRegionMap.clear();
        fillBorder();
        buildHeader();

        List<ProtectionRegion> regions = new ArrayList<>(manager.getRegionsByOwner(ownerPlayer.getUniqueId()));

        if (regions.isEmpty()) {
            buildEmptyState();
            buildFooter(regions.size(), 1);
            return;
        }

        int totalPages = (int) Math.ceil((double) regions.size() / ITEMS_PER_PAGE);
        if (page > totalPages) {
            plugin.getMenuManager().openMenu(ownerPlayer, new BulkManageMenu(plugin, ownerPlayer, totalPages, selectedRegionIds));
            return;
        }

        buildRegionItems(regions);
        buildFooter(regions.size(), totalPages);
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

    private void buildHeader() {
        List<ProtectionRegion> allRegions = manager.getRegionsByOwner(ownerPlayer.getUniqueId());
        int selectedCount = 0;
        for (ProtectionRegion r : allRegions) {
            if (selectedRegionIds.contains(r.getDatabaseId())) {
                selectedCount++;
            }
        }

        List<String> processedLore = new ArrayList<>();
        for (String line : statsLore) {
            processedLore.add(line
                    .replace("%total%", String.valueOf(allRegions.size()))
                    .replace("%selected%", String.valueOf(selectedCount)));
        }

        ItemStack statsItem = new ItemBuilder(statsMaterial)
                .setDisplayName(statsDisplayName)
                .setLore(processedLore.toArray(String[]::new))
                .build();
        if (statsCustomModelData != -1) {
            ItemMeta meta = statsItem.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(statsCustomModelData);
                statsItem.setItemMeta(meta);
            }
        }
        inventory.setItem(4, statsItem);

        ItemStack separator = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName("<dark_gray> ")
                .build();
        inventory.setItem(3, separator);
        inventory.setItem(5, separator);
        inventory.setItem(2, separator.clone());
        inventory.setItem(6, separator.clone());
    }

    private void buildEmptyState() {
        ItemStack emptyItem = new ItemBuilder(Material.BARRIER)
                .setDisplayName(MessageUtils.lang("bulk-menu.empty-btn"))
                .setLore(
                        MessageUtils.lang("bulk-menu.empty-lore-1"),
                        MessageUtils.lang("bulk-menu.empty-lore-2"),
                        MessageUtils.lang("bulk-menu.empty-lore-3")
                )
                .build();
        inventory.setItem(22, emptyItem);
    }

    private void buildRegionItems(List<ProtectionRegion> regions) {
        var blocksConfig = plugin.getBlocksConfig();
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, regions.size());

        for (int i = startIndex, slotIndex = 0; i < endIndex && slotIndex < VALID_SLOTS.length; i++, slotIndex++) {
            ProtectionRegion region = regions.get(i);
            int slot = VALID_SLOTS[slotIndex];

            String typeId = region.getTypeId();
            Material material = Material.BEACON;
            String skullValue = null;
            int customModelData = regionItemCustomModelData;

            if (blocksConfig.contains("protection-blocks." + typeId)) {
                String matName = blocksConfig.getString("protection-blocks." + typeId + ".material");
                if (matName != null) {
                    try { material = Material.valueOf(matName); } catch (IllegalArgumentException ignored) {}
                }
                skullValue = blocksConfig.getString("protection-blocks." + typeId + ".skull-value", "");
                if (customModelData == -1) {
                    customModelData = blocksConfig.getInt("protection-blocks." + typeId + ".custom-model-data", -1);
                }
            }

            boolean isSelected = selectedRegionIds.contains(region.getDatabaseId());
            int radius = region.getRadius();
            String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);

            String displayName = regionItemDisplayName
                    .replace("%name%", region.getCustomName())
                    .replace("%land_id%", region.getLandId())
                    .replace("%world%", region.getWorldName())
                    .replace("%x%", String.valueOf(region.getBlockX()))
                    .replace("%y%", String.valueOf(region.getBlockY()))
                    .replace("%z%", String.valueOf(region.getBlockZ()))
                    .replace("%size%", size)
                    .replace("%members%", String.valueOf(region.getMembers().size()))
                    .replace("%status%", isSelected ? MessageUtils.lang("bulk-menu.region-status-selected") : MessageUtils.lang("bulk-menu.region-status-not-selected"));

            List<String> lore = new ArrayList<>();
            for (String line : regionItemLore) {
                lore.add(line
                        .replace("%name%", region.getCustomName())
                        .replace("%land_id%", region.getLandId())
                        .replace("%world%", region.getWorldName())
                        .replace("%x%", String.valueOf(region.getBlockX()))
                        .replace("%y%", String.valueOf(region.getBlockY()))
                        .replace("%z%", String.valueOf(region.getBlockZ()))
                        .replace("%size%", size)
                        .replace("%members%", String.valueOf(region.getMembers().size()))
                        .replace("%status%", isSelected ? MessageUtils.lang("bulk-menu.region-status-selected") : MessageUtils.lang("bulk-menu.region-status-not-selected")));
            }

            ItemStack regionItem;
            if (material == Material.PLAYER_HEAD && skullValue != null && !skullValue.isEmpty()) {
                regionItem = SkullUtils.createSkullWithTexture(skullValue);
            } else {
                regionItem = new ItemStack(material);
            }

            ItemMeta meta = regionItem.getItemMeta();
            if (meta != null) {
                meta.displayName(mm.deserialize(displayName));
                List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(mm.deserialize(line));
                }
                meta.lore(loreComponents);
                if (customModelData != -1) {
                    meta.setCustomModelData(customModelData);
                }
                if (isSelected) {
                    List<net.kyori.adventure.text.Component> currentLore = meta.lore();
                    if (currentLore != null) {
                        currentLore.add(mm.deserialize("<green>✔ " + MessageUtils.lang("bulk-menu.region-status-selected")));
                        meta.lore(currentLore);
                    }
                }
                regionItem.setItemMeta(meta);
            }

            inventory.setItem(slot, regionItem);
            slotToRegionMap.put(slot, region);
        }
    }

    private void buildFooter(int totalRegions, int totalPages) {
        int showing = Math.min(totalRegions, ITEMS_PER_PAGE);
        int selectedCount = selectedRegionIds.size();

        List<String> processedPageLore = new ArrayList<>();
        for (String line : pageInfoLore) {
            processedPageLore.add(line
                    .replace("%current%", String.valueOf(page))
                    .replace("%total%", String.valueOf(totalPages))
                    .replace("%showing%", String.valueOf(showing))
                    .replace("%totalRegions%", String.valueOf(totalRegions))
                    .replace("%selected%", String.valueOf(selectedCount)));
        }

        ItemStack pageInfo = new ItemBuilder(pageInfoMaterial)
                .setDisplayName(pageInfoDisplayName
                        .replace("%current%", String.valueOf(page))
                        .replace("%total%", String.valueOf(totalPages)))
                .setLore(processedPageLore.toArray(String[]::new))
                .build();
        if (pageInfoCustomModelData != -1) {
            ItemMeta meta = pageInfo.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(pageInfoCustomModelData);
                pageInfo.setItemMeta(meta);
            }
        }
        inventory.setItem(pageInfoSlot, pageInfo);

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

        List<String> processedConfirmLore = new ArrayList<>();
        for (String line : confirmLore) {
            processedConfirmLore.add(line.replace("%count%", String.valueOf(selectedCount)));
        }
        ItemStack confirmBtn = new ItemBuilder(confirmMaterial)
                .setDisplayName(confirmDisplayName.replace("%count%", String.valueOf(selectedCount)))
                .setLore(processedConfirmLore.toArray(String[]::new))
                .build();
        if (confirmCustomModelData != -1) {
            ItemMeta meta = confirmBtn.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(confirmCustomModelData);
                confirmBtn.setItemMeta(meta);
            }
        }
        inventory.setItem(confirmSlot, confirmBtn);

        ItemStack selectAllBtn = new ItemBuilder(selectAllMaterial)
                .setDisplayName(selectAllDisplayName)
                .setLore(selectAllLore.toArray(String[]::new))
                .build();
        if (selectAllCustomModelData != -1) {
            ItemMeta meta = selectAllBtn.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(selectAllCustomModelData);
                selectAllBtn.setItemMeta(meta);
            }
        }
        inventory.setItem(selectAllSlot, selectAllBtn);

        if (page > 1) {
            List<String> processedPrevLore = new ArrayList<>();
            for (String line : prevLore) {
                processedPrevLore.add(line.replace("%page%", String.valueOf(page - 1)));
            }
            ItemStack prevBtn = new ItemBuilder(prevMaterial)
                    .setDisplayName(prevDisplayName.replace("%page%", String.valueOf(page - 1)))
                    .setLore(processedPrevLore.toArray(String[]::new))
                    .build();
            if (prevCustomModelData != -1) {
                ItemMeta meta = prevBtn.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(prevCustomModelData);
                    prevBtn.setItemMeta(meta);
                }
            }
            inventory.setItem(prevSlot, prevBtn);
        }

        if (page < totalPages) {
            List<String> processedNextLore = new ArrayList<>();
            for (String line : nextLore) {
                processedNextLore.add(line.replace("%page%", String.valueOf(page + 1)));
            }
            ItemStack nextBtn = new ItemBuilder(nextMaterial)
                    .setDisplayName(nextDisplayName.replace("%page%", String.valueOf(page + 1)))
                    .setLore(processedNextLore.toArray(String[]::new))
                    .build();
            if (nextCustomModelData != -1) {
                ItemMeta meta = nextBtn.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(nextCustomModelData);
                    nextBtn.setItemMeta(meta);
                }
            }
            inventory.setItem(nextSlot, nextBtn);
        }
    }

    @Override
    public void handleMenuClick(int slot, Player player) {
        handleMenuClick(slot, player, ClickType.LEFT);
    }

    @Override
    public void handleMenuClick(int slot, Player player, ClickType clickType) {
        if (slot == closeSlot) {
            Location loc = player.getLocation();
            if (loc == null) return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            return;
        }

        if (slot == prevSlot && page > 1) {
            Location loc = player.getLocation();
            if (loc == null) return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new BulkManageMenu(plugin, player, page - 1, selectedRegionIds));
            return;
        }

        if (slot == nextSlot) {
            List<ProtectionRegion> regions = manager.getRegionsByOwner(player.getUniqueId());
            int totalPages = (int) Math.ceil((double) regions.size() / ITEMS_PER_PAGE);
            if (page < totalPages) {
                Location loc = player.getLocation();
                if (loc == null) return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openMenu(player, new BulkManageMenu(plugin, player, page + 1, selectedRegionIds));
            }
            return;
        }

        if (slot == selectAllSlot) {
            List<ProtectionRegion> allRegions = manager.getRegionsByOwner(player.getUniqueId());
            if (selectedRegionIds.size() == allRegions.size()) {
                selectedRegionIds.clear();
            } else {
                for (ProtectionRegion r : allRegions) {
                    selectedRegionIds.add(r.getDatabaseId());
                }
            }
            Location loc = player.getLocation();
            if (loc == null) return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new BulkManageMenu(plugin, player, page, selectedRegionIds));
            return;
        }

        if (slot == confirmSlot) {
            if (selectedRegionIds.isEmpty()) {
                player.sendMessage(mm.deserialize(MessageUtils.lang("bulk-menu.no-selected")));
                Location loc = player.getLocation();
                if (loc == null) return;
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }
            player.closeInventory();
            openPlayerSearch(player);
            return;
        }

        if (slotToRegionMap.containsKey(slot)) {
            ProtectionRegion clickedRegion = slotToRegionMap.get(slot);
            int dbId = clickedRegion.getDatabaseId();

            if (selectedRegionIds.contains(dbId)) {
                selectedRegionIds.remove(dbId);
            } else {
                selectedRegionIds.add(dbId);
            }

            Location loc = player.getLocation();
            if (loc == null) return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new BulkManageMenu(plugin, player, page, selectedRegionIds));
        }
    }

    private void openPlayerSearch(Player player) {
        player.sendMessage(mm.deserialize(MessageUtils.lang("bulk-menu.search-prompt")));
        plugin.getChatSearchListener().addBulkCallback(player, selectedRegionIds);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    private String getConfigString(ConfigurationSection section, String path, String def) {
        return section != null ? section.getString(path, def) : def;
    }

    private Material getMaterial(ConfigurationSection section, String path, Material def) {
        if (section == null) return def;
        String matName = section.getString(path);
        if (matName == null) return def;
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
