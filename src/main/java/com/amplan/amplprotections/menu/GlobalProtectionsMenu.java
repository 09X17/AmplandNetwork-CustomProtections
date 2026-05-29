package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
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

public class GlobalProtectionsMenu implements MenuManager.CustomMenu {

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
    private final boolean isAdminMode;
    private final int page;
    private final Player targetPlayer;

    private final String menuTitle;
    private final Material borderMaterial;
    private final String borderName;
    private final int borderCustomModelData;
    private final Material statsMaterial;
    private final String statsDisplayName;
    private final List<String> statsLore;
    private final int statsCustomModelData;
    private final Material emptyMaterial;
    private final String emptyDisplayName;
    private final List<String> emptyLore;
    private final int emptyCustomModelData;
    private final int closeSlot;
    private final Material closeMaterial;
    private final String closeDisplayName;
    private final List<String> closeLore;
    private final int closeCustomModelData;
    private final int refreshSlot;
    private final Material refreshMaterial;
    private final String refreshDisplayName;
    private final List<String> refreshLore;
    private final int refreshCustomModelData;
    private final int pageInfoSlot;
    private final Material pageInfoMaterial;
    private final String pageInfoDisplayName;
    private final List<String> pageInfoLore;
    private final int pageInfoCustomModelData;
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

    public GlobalProtectionsMenu(AmplProtections plugin, Player targetPlayer, boolean isAdminMode) {
        this(plugin, targetPlayer, isAdminMode, 1);
    }

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public GlobalProtectionsMenu(AmplProtections plugin, Player targetPlayer, boolean isAdminMode, int page) {
        this.plugin = plugin;
        this.manager = plugin.getProtectionManager();
        this.isAdminMode = isAdminMode;
        this.page = Math.max(1, page);
        this.targetPlayer = targetPlayer;

        org.bukkit.configuration.file.FileConfiguration menuConfig = plugin.getMenuConfigManager().getGlobalMenu();

        String defaultTitle = isAdminMode ? "<red>Panel Admin: Todas las Zonas" : "<dark_gray>Mis Protecciones Activas";
        this.menuTitle = getConfigString(menuConfig, isAdminMode ? "title-admin" : "title-player", defaultTitle);
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(menuTitle));

        ConfigurationSection borderCfg = menuConfig != null ? menuConfig.getConfigurationSection("border") : null;
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);

        ConfigurationSection statsCfg = menuConfig != null ? menuConfig.getConfigurationSection("stats-item") : null;
        this.statsMaterial = getMaterial(statsCfg, "material", Material.BEACON);
        this.statsDisplayName = getConfigString(statsCfg, "display-name", "<gold><b>Estadisticas</b></gold>");
        this.statsLore = getConfigList(statsCfg, "lore", Arrays.asList(
                "<gray>Total del servidor: <white>%total%",
                "<gray>Tus protecciones: <white>%player%",
                "",
                isAdminMode ? "<red>Modo Administrador" : "<green>Modo Jugador"));
        this.statsCustomModelData = getCustomModelData(statsCfg, "custom-model-data", -1);

        ConfigurationSection emptyCfg = menuConfig != null ? menuConfig.getConfigurationSection("empty-item") : null;
        this.emptyMaterial = getMaterial(emptyCfg, "material", Material.BARRIER);
        this.emptyDisplayName = getConfigString(emptyCfg, "display-name", "<red><b>Sin Protecciones</b></red>");
        this.emptyLore = getConfigList(emptyCfg, "lore", Arrays.asList(
                "<gray>No hay terrenos registrados.",
                "",
                "<yellow>Coloca un bloque de proteccion",
                "<yellow>para crear una zona"));
        this.emptyCustomModelData = getCustomModelData(emptyCfg, "custom-model-data", -1);

        ConfigurationSection closeCfg = menuConfig != null ? menuConfig.getConfigurationSection("close-button") : null;
        this.closeSlot = closeCfg != null ? closeCfg.getInt("slot", 49) : 49;
        this.closeMaterial = getMaterial(closeCfg, "material", Material.BARRIER);
        this.closeDisplayName = getConfigString(closeCfg, "display-name", "<red><b>Cerrar</b></red>");
        this.closeLore = getConfigList(closeCfg, "lore", Collections.singletonList("<gray>Clic para salir"));
        this.closeCustomModelData = getCustomModelData(closeCfg, "custom-model-data", -1);

        ConfigurationSection refreshCfg = menuConfig != null ? menuConfig.getConfigurationSection("refresh-button")
                : null;
        this.refreshSlot = refreshCfg != null ? refreshCfg.getInt("slot", 51) : 51;
        this.refreshMaterial = getMaterial(refreshCfg, "material", Material.COMPASS);
        this.refreshDisplayName = getConfigString(refreshCfg, "display-name", "<aqua><b>Actualizar</b></aqua>");
        this.refreshLore = getConfigList(refreshCfg, "lore",
                Collections.singletonList("<gray>Clic para refrescar la lista"));
        this.refreshCustomModelData = getCustomModelData(refreshCfg, "custom-model-data", -1);

        ConfigurationSection pageCfg = menuConfig != null ? menuConfig.getConfigurationSection("page-info") : null;
        this.pageInfoSlot = pageCfg != null ? pageCfg.getInt("slot", 47) : 47;
        this.pageInfoMaterial = getMaterial(pageCfg, "material", Material.KNOWLEDGE_BOOK);
        this.pageInfoDisplayName = getConfigString(pageCfg, "display-name",
                "<white><b>Pagina %current% / %total%</b></white>");
        this.pageInfoLore = getConfigList(pageCfg, "lore", Arrays.asList(
                "<gray>Mostrando: <white>%showing% / %totalRegions% regiones",
                "",
                "<green>Clic en una region para editar"));
        this.pageInfoCustomModelData = getCustomModelData(pageCfg, "custom-model-data", -1);

        ConfigurationSection prevCfg = menuConfig != null ? menuConfig.getConfigurationSection("prev-button") : null;
        this.prevSlot = prevCfg != null ? prevCfg.getInt("slot", 45) : 45;
        this.prevMaterial = getMaterial(prevCfg, "material", Material.ARROW);
        this.prevDisplayName = getConfigString(prevCfg, "display-name", "<green><b>Anterior</b></green>");
        this.prevLore = getConfigList(prevCfg, "lore", Collections.singletonList("<gray>Clic para pagina %page%"));
        this.prevCustomModelData = getCustomModelData(prevCfg, "custom-model-data", -1);

        ConfigurationSection nextCfg = menuConfig != null ? menuConfig.getConfigurationSection("next-button") : null;
        this.nextSlot = nextCfg != null ? nextCfg.getInt("slot", 53) : 53;
        this.nextMaterial = getMaterial(nextCfg, "material", Material.ARROW);
        this.nextDisplayName = getConfigString(nextCfg, "display-name", "<green><b>Siguiente</b></green>");
        this.nextLore = getConfigList(nextCfg, "lore", Collections.singletonList("<gray>Clic para pagina %page%"));
        this.nextCustomModelData = getCustomModelData(nextCfg, "custom-model-data", -1);

        buildInventoryContents();
    }

    private void buildInventoryContents() {
        inventory.clear();
        slotToRegionMap.clear();
        fillBorder();
        buildHeader();

        List<ProtectionRegion> regions = isAdminMode
                ? new ArrayList<>(manager.getAllRegions())
                : new ArrayList<>(manager.getRegionsByOwner(targetPlayer.getUniqueId()));

        if (regions.isEmpty()) {
            buildEmptyState();
            buildFooter(regions.size(), 1);
            return;
        }

        int totalPages = (int) Math.ceil((double) regions.size() / ITEMS_PER_PAGE);
        if (page > totalPages) {
            plugin.getMenuManager().openMenu(targetPlayer,
                    new GlobalProtectionsMenu(plugin, targetPlayer, isAdminMode, totalPages));
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
        for (int i = 0; i < 9; i++)
            inventory.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, border);
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    private void buildHeader() {
        List<ProtectionRegion> allRegions = manager.getAllRegions();
        int totalRegions = allRegions.size();
        int playerRegions = manager.getRegionsByOwner(targetPlayer.getUniqueId()).size();

        List<String> processedLore = new ArrayList<>();
        for (String line : statsLore) {
            processedLore.add(line
                    .replace("%total%", String.valueOf(totalRegions))
                    .replace("%player%", String.valueOf(playerRegions))
                    .replace("%mode%", isAdminMode ? "<red>Modo Administrador" : "<green>Modo Jugador"));
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
        ItemStack emptyItem = new ItemBuilder(emptyMaterial)
                .setDisplayName(emptyDisplayName)
                .setLore(emptyLore.toArray(String[]::new))
                .build();
        if (emptyCustomModelData != -1) {
            ItemMeta meta = emptyItem.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(emptyCustomModelData);
                emptyItem.setItemMeta(meta);
            }
        }
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
            int customModelData = -1;

            if (blocksConfig.contains("protection-blocks." + typeId)) {
                String matName = blocksConfig.getString("protection-blocks." + typeId + ".material");
                if (matName != null) {
                    try {
                        material = Material.valueOf(matName);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                customModelData = blocksConfig.getInt("protection-blocks." + typeId + ".custom-model-data", -1);
            }

            int radius = region.getRadius();
            String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);
            long enabledFlags = region.getFlags().values().stream()
                    .filter(l -> l != com.amplan.amplprotections.model.FlagPermissionLevel.NONE).count();
            int totalFlags = region.getFlags().size();

            List<String> lore = new ArrayList<>();
            lore.add("<gray>ID: <yellow>" + region.getLandId());
            lore.add("<gray>Mundo: <green>" + region.getWorldName());
            lore.add("<gray>Centro: <aqua>" + region.getBlockX() + ", " + region.getBlockY() + ", "
                    + region.getBlockZ());
            lore.add("<gray>Tamano: <yellow>" + size);
            lore.add("");
            lore.add("<dark_gray>Flags: <green>" + enabledFlags + "<dark_gray>/<gray>" + totalFlags);
            lore.add("<gray>Miembros: <white>" + region.getMembers().size());

            if (isAdminMode) {
                UUID ownerUuid = region.getOwnerUniqueId();
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
                lore.add("");
                lore.add(
                        "<red><b>Dueno: <yellow>" + (owner.getName() != null ? owner.getName() : ownerUuid.toString()));
            }

            lore.add("");
            lore.add("<yellow>Clic para administrar");

            ItemStack regionItem = new ItemBuilder(material)
                    .setDisplayName("<gold><b>" + region.getCustomName() + "</b></gold>")
                    .setLore(lore.toArray(String[]::new))
                    .build();

            ItemMeta meta = regionItem.getItemMeta();
            if (meta != null && customModelData != -1) {
                meta.setCustomModelData(customModelData);
                regionItem.setItemMeta(meta);
            }

            inventory.setItem(slot, regionItem);
            slotToRegionMap.put(slot, region);
        }
    }

    private void buildFooter(int totalRegions, int totalPages) {
        int showing = Math.min(totalRegions, ITEMS_PER_PAGE);

        List<String> processedPageLore = new ArrayList<>();
        for (String line : pageInfoLore) {
            processedPageLore.add(line
                    .replace("%current%", String.valueOf(page))
                    .replace("%total%", String.valueOf(totalPages))
                    .replace("%showing%", String.valueOf(showing))
                    .replace("%totalRegions%", String.valueOf(totalRegions)));
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

        ItemStack refreshBtn = new ItemBuilder(refreshMaterial)
                .setDisplayName(refreshDisplayName)
                .setLore(refreshLore.toArray(String[]::new))
                .build();
        if (refreshCustomModelData != -1) {
            ItemMeta meta = refreshBtn.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(refreshCustomModelData);
                refreshBtn.setItemMeta(meta);
            }
        }
        inventory.setItem(refreshSlot, refreshBtn);

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
        if (slot == closeSlot) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            return;
        }

        if (slot == refreshSlot) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new GlobalProtectionsMenu(plugin, player, isAdminMode, page));
            return;
        }

        if (slot == prevSlot && page > 1) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new GlobalProtectionsMenu(plugin, player, isAdminMode, page - 1));
            return;
        }

        if (slot == nextSlot) {
            List<ProtectionRegion> regions = isAdminMode
                    ? manager.getAllRegions()
                    : manager.getRegionsByOwner(player.getUniqueId());
            int totalPages = (int) Math.ceil((double) regions.size() / ITEMS_PER_PAGE);
            if (page < totalPages) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openMenu(player,
                        new GlobalProtectionsMenu(plugin, player, isAdminMode, page + 1));
            }
            return;
        }

        if (slotToRegionMap.containsKey(slot)) {
            ProtectionRegion selectedRegion = slotToRegionMap.get(slot);
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            plugin.getMenuManager().openMenu(player, new MainProtectionMenu(plugin, selectedRegion));
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
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
