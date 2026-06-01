package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.MessageUtils;
import com.amplan.amplprotections.utils.SkullUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class AdminMenu implements MenuManager.CustomMenu {

    private static final class MenuHolder implements InventoryHolder {
        private AdminMenu menu;

        void setMenu(AdminMenu m) {
            this.menu = m;
        }

        @Override
        public Inventory getInventory() {
            return menu != null ? menu.inventory : null;
        }
    }

    private static final int ITEMS_PER_PAGE = 21;
    private static final int[] VALID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final AmplProtections plugin;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<Integer, ProtectionRegion> slotToRegion = new HashMap<>();
    private final int page;
    private final Player viewer;

    private final String menuTitle;
    private final Material borderMaterial;
    private final String borderDisplayName;
    private final List<String> borderLore;
    private final int borderCustomModelData;
    private final String borderSkullValue;
    private final Material separatorMaterial;
    private final String separatorDisplayName;
    private final int separatorCustomModelData;
    private final String separatorSkullValue;
    private final Material statsMaterial;
    private final String statsDisplayName;
    private final List<String> statsLore;
    private final int statsSlot;
    private final int statsCustomModelData;
    private final String statsSkullValue;
    private final Material searchMaterial;
    private final String searchDisplayName;
    private final List<String> searchLore;
    private final int searchSlot;
    private final int searchCustomModelData;
    private final String searchSkullValue;
    private final Material regionMaterial;
    private final String regionDisplayName;
    private final List<String> regionLore;
    private final int regionCustomModelData;
    private final String regionSkullValue;
    private final Material emptyMaterial;
    private final String emptyDisplayName;
    private final List<String> emptyLore;
    private final int emptyCustomModelData;
    private final String emptySkullValue;
    private final int backSlot;
    private final Material backMaterial;
    private final String backDisplayName;
    private final List<String> backLore;
    private final int backCustomModelData;
    private final String backSkullValue;
    private final int closeSlot;
    private final Material closeMaterial;
    private final String closeDisplayName;
    private final List<String> closeLore;
    private final int closeCustomModelData;
    private final String closeSkullValue;
    private final int refreshSlot;
    private final Material refreshMaterial;
    private final String refreshDisplayName;
    private final List<String> refreshLore;
    private final int refreshCustomModelData;
    private final String refreshSkullValue;
    private final int pageInfoSlot;
    private final Material pageInfoMaterial;
    private final String pageInfoDisplayName;
    private final List<String> pageInfoLore;
    private final int pageInfoCustomModelData;
    private final String pageInfoSkullValue;
    private final int prevSlot;
    private final Material prevMaterial;
    private final String prevDisplayName;
    private final List<String> prevLore;
    private final int prevCustomModelData;
    private final String prevSkullValue;
    private final int nextSlot;
    private final Material nextMaterial;
    private final String nextDisplayName;
    private final List<String> nextLore;
    private final int nextCustomModelData;
    private final String nextSkullValue;


    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public AdminMenu(AmplProtections plugin, Player viewer, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.page = Math.max(1, page);

        FileConfiguration config = plugin.getAdminMenuConfig();

        this.menuTitle = getConfigString(config, "title", MessageUtils.lang("admin-menu.title"));

        ConfigurationSection borderCfg = config.getConfigurationSection("border");
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderDisplayName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderLore = getConfigList(borderCfg, "lore", Collections.emptyList());
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);
        this.borderSkullValue = getConfigString(borderCfg, "skull-value", null);

        ConfigurationSection sepCfg = config.getConfigurationSection("separator");
        this.separatorMaterial = getMaterial(sepCfg, "material", Material.GRAY_STAINED_GLASS_PANE);
        this.separatorDisplayName = getConfigString(sepCfg, "display-name", "<dark_gray> ");
        this.separatorCustomModelData = getCustomModelData(sepCfg, "custom-model-data", -1);
        this.separatorSkullValue = getConfigString(sepCfg, "skull-value", null);

        ConfigurationSection statsCfg = config.getConfigurationSection("stats");
        this.statsSlot = statsCfg != null ? statsCfg.getInt("slot", 4) : 4;
        this.statsMaterial = getMaterial(statsCfg, "material", Material.BEACON);
        this.statsDisplayName = getConfigString(statsCfg, "display-name",
                "<gold><b>Estadísticas del Servidor</b></gold>");
        this.statsLore = getConfigList(statsCfg, "lore", Arrays.asList(
                "",
                " <reset><yellow>▸ <gray>Total protecciones: <white>%total%",
                " <reset><yellow>▸ <gray>Jugadores únicos: <white>%unique_players%",
                " <reset><yellow>▸ <gray>Tus protecciones: <white>%player%",
                "",
                " <reset><red><b>Modo Administrador</b>"));
        this.statsCustomModelData = getCustomModelData(statsCfg, "custom-model-data", -1);
        this.statsSkullValue = getConfigString(statsCfg, "skull-value", null);

        ConfigurationSection searchCfg = config.getConfigurationSection("search");
        this.searchSlot = searchCfg != null ? searchCfg.getInt("slot", 2) : 2;
        this.searchMaterial = getMaterial(searchCfg, "material", Material.OAK_SIGN);
        this.searchDisplayName = getConfigString(searchCfg, "display-name", MessageUtils.lang("admin-menu.search-btn"));
        this.searchLore = getConfigList(searchCfg, "lore", Arrays.asList(
                "",
                " <reset><yellow>▸ <gray>Haz clic para buscar</gray>",
                " <reset><yellow>▸ <gray>protecciones por nombre</gray>",
                "",
                " <reset><green>Clic para abrir buscador"));
        this.searchCustomModelData = getCustomModelData(searchCfg, "custom-model-data", -1);
        this.searchSkullValue = getConfigString(searchCfg, "skull-value", null);

        ConfigurationSection regionCfg = config.getConfigurationSection("region-item");
        this.regionMaterial = getMaterial(regionCfg, "material", Material.BEACON);
        this.regionDisplayName = getConfigString(regionCfg, "display-name", "<gold><b>%name%</b></gold>");
        this.regionLore = getConfigList(regionCfg, "lore", Arrays.asList(
                "",
                " <reset><yellow>▸ <gray>ID: <aqua>%land_id%",
                " <reset><yellow>▸ <gray>Tipo: <white>%type%",
                " <reset><yellow>▸ <gray>Mundo: <green>%world%",
                " <reset><yellow>▸ <gray>Centro: <aqua>X:%x% Y:%y% Z:%z%",
                " <reset><yellow>▸ <gray>Tamaño: <yellow>%size%",
                "",
                " <reset><yellow>▸ <gray>Flags activas: <green>%enabled_flags%",
                " <reset><yellow>▸ <gray>Miembros: <white>%members%",
                "",
                " <reset><red><b>Dueño: <yellow>%owner%",
                "",
                " <reset><yellow>Clic para administrar",
                " <reset><red>Shift + Clic para teletransportar"));
        this.regionCustomModelData = getCustomModelData(regionCfg, "custom-model-data", -1);
        this.regionSkullValue = getConfigString(regionCfg, "skull-value", null);

        ConfigurationSection emptyCfg = config.getConfigurationSection("empty");
        this.emptyMaterial = getMaterial(emptyCfg, "material", Material.BARRIER);
        this.emptyDisplayName = getConfigString(emptyCfg, "display-name", MessageUtils.lang("admin-menu.no-protections"));
        this.emptyLore = getConfigList(emptyCfg, "lore", Arrays.asList(
                "",
                " <reset><gray>No hay terrenos registrados.",
                "",
                " <reset><yellow>Usa /aprot give para crear una"));
        this.emptyCustomModelData = getCustomModelData(emptyCfg, "custom-model-data", -1);
        this.emptySkullValue = getConfigString(emptyCfg, "skull-value", null);

        ConfigurationSection backCfg = config.getConfigurationSection("back");
        this.backSlot = backCfg != null ? backCfg.getInt("slot", 45) : 45;
        this.backMaterial = getMaterial(backCfg, "material", Material.ARROW);
        this.backDisplayName = getConfigString(backCfg, "display-name", "<green><b>Volver</b></green>");
        this.backLore = getConfigList(backCfg, "lore", Collections.singletonList(""));
        this.backCustomModelData = getCustomModelData(backCfg, "custom-model-data", -1);
        this.backSkullValue = getConfigString(backCfg, "skull-value", null);

        ConfigurationSection closeCfg = config.getConfigurationSection("close");
        this.closeSlot = closeCfg != null ? closeCfg.getInt("slot", 49) : 49;
        this.closeMaterial = getMaterial(closeCfg, "material", Material.BARRIER);
        this.closeDisplayName = getConfigString(closeCfg, "display-name", "<red><b>Cerrar</b></red>");
        this.closeLore = getConfigList(closeCfg, "lore", Collections.singletonList(""));
        this.closeCustomModelData = getCustomModelData(closeCfg, "custom-model-data", -1);
        this.closeSkullValue = getConfigString(closeCfg, "skull-value", null);

        ConfigurationSection refreshCfg = config.getConfigurationSection("refresh");
        this.refreshSlot = refreshCfg != null ? refreshCfg.getInt("slot", 51) : 51;
        this.refreshMaterial = getMaterial(refreshCfg, "material", Material.COMPASS);
        this.refreshDisplayName = getConfigString(refreshCfg, "display-name", "<aqua><b>Actualizar</b></aqua>");
        this.refreshLore = getConfigList(refreshCfg, "lore", Collections.singletonList(""));
        this.refreshCustomModelData = getCustomModelData(refreshCfg, "custom-model-data", -1);
        this.refreshSkullValue = getConfigString(refreshCfg, "skull-value", null);

        ConfigurationSection pageCfg = config.getConfigurationSection("page-info");
        this.pageInfoSlot = pageCfg != null ? pageCfg.getInt("slot", 47) : 47;
        this.pageInfoMaterial = getMaterial(pageCfg, "material", Material.KNOWLEDGE_BOOK);
        this.pageInfoDisplayName = getConfigString(pageCfg, "display-name",
                "<white><b>Página %current% / %total%</b></white>");
        this.pageInfoLore = getConfigList(pageCfg, "lore", Arrays.asList(
                "",
                " <reset><yellow>▸ <gray>Mostrando: <white>%showing% / %total_regions%",
                "",
                " <reset><green>Clic en una región para editar"));
        this.pageInfoCustomModelData = getCustomModelData(pageCfg, "custom-model-data", -1);
        this.pageInfoSkullValue = getConfigString(pageCfg, "skull-value", null);

        ConfigurationSection prevCfg = config.getConfigurationSection("prev-page");
        this.prevSlot = prevCfg != null ? prevCfg.getInt("slot", 46) : 46;
        this.prevMaterial = getMaterial(prevCfg, "material", Material.ARROW);
        this.prevDisplayName = getConfigString(prevCfg, "display-name", "<green><b>Anterior</b></green>");
        this.prevLore = getConfigList(prevCfg, "lore", Collections.singletonList(""));
        this.prevCustomModelData = getCustomModelData(prevCfg, "custom-model-data", -1);
        this.prevSkullValue = getConfigString(prevCfg, "skull-value", null);

        ConfigurationSection nextCfg = config.getConfigurationSection("next-page");
        this.nextSlot = nextCfg != null ? nextCfg.getInt("slot", 52) : 52;
        this.nextMaterial = getMaterial(nextCfg, "material", Material.ARROW);
        this.nextDisplayName = getConfigString(nextCfg, "display-name", "<green><b>Siguiente</b></green>");
        this.nextLore = getConfigList(nextCfg, "lore", Collections.singletonList(""));
        this.nextCustomModelData = getCustomModelData(nextCfg, "custom-model-data", -1);
        this.nextSkullValue = getConfigString(nextCfg, "skull-value", null);

        Component titleComponent = mm.deserialize(menuTitle);

        MenuHolder holder = new MenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, titleComponent);
        holder.setMenu(this);
        this.inventory = inv;
        buildInventoryContents();
    }

    private void buildInventoryContents() {
        inventory.clear();
        slotToRegion.clear();
        fillBorder();
        buildHeader();

        List<ProtectionRegion> allRegions = new ArrayList<>(plugin.getProtectionManager().getAllRegions());

        if (allRegions.isEmpty()) {
            buildEmptyState();
            buildFooter(0, 1);
            return;
        }

        int totalPages = (int) Math.ceil((double) allRegions.size() / ITEMS_PER_PAGE);
        int safePage = Math.min(page, Math.max(1, totalPages));

        if (page > totalPages) {
            plugin.getMenuManager().openMenu(viewer, new AdminMenu(plugin, viewer, totalPages));
            return;
        }

        int start = (safePage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allRegions.size());

        for (int i = start, slotIndex = 0; i < end && slotIndex < VALID_SLOTS.length; i++, slotIndex++) {
            ProtectionRegion region = allRegions.get(i);
            int slot = VALID_SLOTS[slotIndex];
            buildRegionItem(region, slot);
        }

        buildFooter(allRegions.size(), totalPages);
    }

    private void fillBorder() {
        ItemStack border = buildItem(borderMaterial, borderDisplayName, borderLore, borderCustomModelData,
                borderSkullValue);
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
        List<ProtectionRegion> allRegions = plugin.getProtectionManager().getAllRegions();
        int totalRegions = allRegions.size();
        Set<UUID> uniqueOwners = allRegions.stream()
                .map(ProtectionRegion::getOwnerUniqueId)
                .collect(Collectors.toSet());
        int uniquePlayers = uniqueOwners.size();
        int playerRegions = plugin.getProtectionManager().getRegionsByOwner(viewer.getUniqueId()).size();

        List<String> processedLore = new ArrayList<>();
        for (String line : statsLore) {
            processedLore.add(line
                    .replace("%total%", String.valueOf(totalRegions))
                    .replace("%unique_players%", String.valueOf(uniquePlayers))
                    .replace("%player%", String.valueOf(playerRegions)));
        }

        ItemStack stats = buildItem(statsMaterial, statsDisplayName, processedLore, statsCustomModelData,
                statsSkullValue);
        inventory.setItem(statsSlot, stats);

        ItemStack searchBtn = buildItem(searchMaterial, searchDisplayName, searchLore, searchCustomModelData,
                searchSkullValue);
        inventory.setItem(searchSlot, searchBtn);

        ItemStack separator = buildItem(separatorMaterial, separatorDisplayName, Collections.emptyList(),
                separatorCustomModelData, separatorSkullValue);
        inventory.setItem(3, separator);
        inventory.setItem(5, separator);
        inventory.setItem(2, separator.clone());
        inventory.setItem(6, separator.clone());
    }

    private void buildEmptyState() {
        ItemStack empty = buildItem(emptyMaterial, emptyDisplayName, emptyLore, emptyCustomModelData, emptySkullValue);
        inventory.setItem(22, empty);
    }

    private void buildRegionItem(ProtectionRegion region, int slot) {
        int radius = region.getRadius();
        String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);
        long enabledFlags = region.getFlags().values().stream()
                .filter(l -> l != com.amplan.amplprotections.model.FlagPermissionLevel.NONE).count();
        long totalFlags = region.getFlags().size();

        List<String> processedLore = new ArrayList<>();
        for (String line : regionLore) {
            processedLore.add(line
                    .replace("%name%", region.getCustomName())
                    .replace("%land_id%", region.getLandId())
                    .replace("%type%", region.getTypeId().replace("_", " ").toUpperCase())
                    .replace("%world%", region.getWorldName())
                    .replace("%x%", String.valueOf(region.getBlockX()))
                    .replace("%y%", String.valueOf(region.getBlockY()))
                    .replace("%z%", String.valueOf(region.getBlockZ()))
                    .replace("%size%", size)
                    .replace("%enabled_flags%", String.valueOf(enabledFlags))
                    .replace("%total_flags%", String.valueOf(totalFlags))
                    .replace("%members%", String.valueOf(region.getMembers().size()))
                    .replace("%owner%", region.getOwnerName()));
        }

        String resolvedName = regionDisplayName
                .replace("%name%", region.getCustomName())
                .replace("%land_id%", region.getLandId())
                .replace("%type%", region.getTypeId().replace("_", " ").toUpperCase())
                .replace("%world%", region.getWorldName())
                .replace("%x%", String.valueOf(region.getBlockX()))
                .replace("%y%", String.valueOf(region.getBlockY()))
                .replace("%z%", String.valueOf(region.getBlockZ()))
                .replace("%size%", size)
                .replace("%enabled_flags%", String.valueOf(enabledFlags))
                .replace("%total_flags%", String.valueOf(totalFlags))
                .replace("%members%", String.valueOf(region.getMembers().size()))
                .replace("%owner%", region.getOwnerName());

        ItemStack item = buildItem(regionMaterial, resolvedName, processedLore, regionCustomModelData, regionSkullValue,
                region);
        inventory.setItem(slot, item);
        slotToRegion.put(slot, region);
    }

    private void buildFooter(int totalRegions, int totalPages) {
        int showing = Math.min(totalRegions, ITEMS_PER_PAGE);

        List<String> processedPageLore = new ArrayList<>();
        for (String line : pageInfoLore) {
            processedPageLore.add(line
                    .replace("%current%", String.valueOf(page))
                    .replace("%total%", String.valueOf(totalPages))
                    .replace("%showing%", String.valueOf(showing))
                    .replace("%total_regions%", String.valueOf(totalRegions)));
        }
        ItemStack pageInfo = buildItem(pageInfoMaterial,
                pageInfoDisplayName.replace("%current%", String.valueOf(page)).replace("%total%",
                        String.valueOf(totalPages)),
                processedPageLore, pageInfoCustomModelData, pageInfoSkullValue);
        inventory.setItem(pageInfoSlot, pageInfo);

        ItemStack backBtn = buildItem(backMaterial, backDisplayName, backLore, backCustomModelData, backSkullValue);
        inventory.setItem(backSlot, backBtn);

        ItemStack closeBtn = buildItem(closeMaterial, closeDisplayName, closeLore, closeCustomModelData,
                closeSkullValue);
        inventory.setItem(closeSlot, closeBtn);

        ItemStack refreshBtn = buildItem(refreshMaterial, refreshDisplayName, refreshLore, refreshCustomModelData,
                refreshSkullValue);
        inventory.setItem(refreshSlot, refreshBtn);

        if (page > 1) {
            List<String> processedPrevLore = new ArrayList<>();
            for (String line : prevLore) {
                processedPrevLore.add(line.replace("%page%", String.valueOf(page - 1)));
            }
            ItemStack prevBtn = buildItem(prevMaterial, prevDisplayName, processedPrevLore, prevCustomModelData,
                    prevSkullValue);
            inventory.setItem(prevSlot, prevBtn);
        }

        if (page < totalPages) {
            List<String> processedNextLore = new ArrayList<>();
            for (String line : nextLore) {
                processedNextLore.add(line.replace("%page%", String.valueOf(page + 1)));
            }
            ItemStack nextBtn = buildItem(nextMaterial, nextDisplayName, processedNextLore, nextCustomModelData,
                    nextSkullValue);
            inventory.setItem(nextSlot, nextBtn);
        }
    }

    private ItemStack buildItem(Material material, String displayName, List<String> lore, int customModelData,
            String skullValue) {
        return buildItem(material, displayName, lore, customModelData, skullValue, null);
    }

    private ItemStack buildItem(Material material, String displayName, List<String> lore, int customModelData,
            String skullValue, ProtectionRegion region) {
        ItemStack item;
        if (material == Material.PLAYER_HEAD && skullValue != null && !skullValue.isEmpty()) {
            item = SkullUtils.createSkullWithTexture(skullValue);
        } else if (material == Material.PLAYER_HEAD && region != null) {
            item = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(region.getOwnerUniqueId()));
                item.setItemMeta(skullMeta);
            }
        } else {
            item = new ItemStack(material);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(displayName));

            if (!lore.isEmpty()) {
                List<net.kyori.adventure.text.Component> components = new ArrayList<>();
                for (String line : lore) {
                    components.add(mm.deserialize(line));
                }
                meta.lore(components);
            }

            if (customModelData != -1) {
                meta.setCustomModelData(customModelData);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleMenuClick(int slot, Player player) {
        handleMenuClick(slot, player, ClickType.LEFT);
    }

    @Override
    public void handleMenuClick(int slot, Player player, ClickType clickType) {
        Location loc = player.getLocation();
        if (loc == null)
            return;

        if (slot == closeSlot) {
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            return;
        }

        if (slot == searchSlot) {
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            plugin.getChatSearchListener().startSearch(player);
            return;
        }

        if (slot == refreshSlot) {
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new AdminMenu(plugin, player, page));
            return;
        }

        if (slot == prevSlot && page > 1) {
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new AdminMenu(plugin, player, page - 1));
            return;
        }

        if (slot == nextSlot) {
            List<ProtectionRegion> allRegions = plugin.getProtectionManager().getAllRegions();
            int totalPages = (int) Math.ceil((double) allRegions.size() / ITEMS_PER_PAGE);
            if (page < totalPages) {
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openMenu(player, new AdminMenu(plugin, player, page + 1));
            }
            return;
        }

        if (slotToRegion.containsKey(slot)) {
            ProtectionRegion selected = slotToRegion.get(slot);
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                player.closeInventory();
                Location tpLoc = new Location(
                        Bukkit.getWorld(selected.getWorldName()),
                        selected.getBlockX() + 0.5, selected.getBlockY() + 1, selected.getBlockZ() + 0.5);
                player.teleport(tpLoc);
                player.sendMessage(mm.deserialize("<green>Teletransportado a <yellow>" + selected.getCustomName()));
                player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            } else {
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
                plugin.getMenuManager().openMenu(player, new MainProtectionMenu(plugin, selected));
            }
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
