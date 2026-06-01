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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.utils.SkullUtils;
import com.amplan.amplprotections.utils.MessageUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;

@SuppressWarnings("ThisEscapedInObjectConstruction")
public class SearchMenu implements MenuManager.CustomMenu, Listener {

    private static final int[] REGION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final AmplProtections plugin;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<Integer, OfflinePlayer> slotToPlayer = new HashMap<>();
    private final int page;
    private final List<OfflinePlayer> searchResults;

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
    private final Material searchHeaderMaterial;
    private final String searchHeaderDisplayName;
    private final List<String> searchHeaderLore;
    private final int searchHeaderSlot;
    private final int searchHeaderCustomModelData;
    private final String searchHeaderSkullValue;
    private final String playerHeadDisplayName;
    private final List<String> playerHeadLore;
    private final int playerHeadCustomModelData;
    private final Material searchEmptyMaterial;
    private final String searchEmptyDisplayName;
    private final List<String> searchEmptyLore;
    private final int searchEmptyCustomModelData;
    private final String searchEmptySkullValue;
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

    public SearchMenu(AmplProtections plugin, List<OfflinePlayer> players, int page) {
        this.plugin = plugin;
        this.searchResults = new ArrayList<>(players);
        this.page = Math.max(1, page);

        FileConfiguration config = plugin.getAdminMenuConfig();

        this.menuTitle = getConfigString(config, "title", MessageUtils.lang("search-menu.title"));

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

        ConfigurationSection headerCfg = config.getConfigurationSection("search-header");
        this.searchHeaderSlot = headerCfg != null ? headerCfg.getInt("slot", 4) : 4;
        this.searchHeaderMaterial = getMaterial(headerCfg, "material", Material.KNOWLEDGE_BOOK);
        this.searchHeaderDisplayName = getConfigString(headerCfg, "display-name",
                MessageUtils.lang("search-menu.header-btn"));
        this.searchHeaderLore = getConfigList(headerCfg, "lore", Arrays.asList(
                "",
                MessageUtils.lang("search-menu.header-lore-searching"),
                MessageUtils.lang("search-menu.header-lore-found"),
                MessageUtils.lang("search-menu.header-lore-total"),
                "",
                MessageUtils.lang("search-menu.header-lore-click")));
        this.searchHeaderCustomModelData = getCustomModelData(headerCfg, "custom-model-data", -1);
        this.searchHeaderSkullValue = getConfigString(headerCfg, "skull-value", null);

        ConfigurationSection playerCfg = config.getConfigurationSection("player-search-item");
        this.playerHeadDisplayName = getConfigString(playerCfg, "display-name", "<gold><b>%player_name%</b></gold>");
        this.playerHeadLore = getConfigList(playerCfg, "lore", Arrays.asList(
                "",
                " <reset><yellow>▸ <gray>UUID: <aqua>%player_uuid%",
                " <reset><yellow>▸ <gray>Protecciones: <white>%protections%",
                "",
                " <reset><yellow>Clic para ver protecciones"));
        this.playerHeadCustomModelData = getCustomModelData(playerCfg, "custom-model-data", -1);

        ConfigurationSection emptyCfg = config.getConfigurationSection("search-empty");
        this.searchEmptyMaterial = getMaterial(emptyCfg, "material", Material.PLAYER_HEAD);
        this.searchEmptyDisplayName = getConfigString(emptyCfg, "display-name",
                MessageUtils.lang("search-menu.empty-btn"));
        this.searchEmptyLore = getConfigList(emptyCfg, "lore", Arrays.asList(
                "",
                MessageUtils.lang("search-menu.empty-lore-1"),
                MessageUtils.lang("search-menu.empty-lore-2") + " <white>%search%",
                "",
                MessageUtils.lang("search-menu.empty-lore-3")));
        this.searchEmptyCustomModelData = getCustomModelData(emptyCfg, "custom-model-data", -1);
        this.searchEmptySkullValue = getConfigString(emptyCfg, "skull-value", null);

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

        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(menuTitle));

        buildInventoryContents();
    }

    private void buildInventoryContents() {
        inventory.clear();
        slotToPlayer.clear();
        fillBorder();
        buildHeader();

        if (searchResults.isEmpty()) {
            buildEmptyState();
            buildFooter(0, 1);
            return;
        }

        int totalPages = (int) Math.ceil((double) searchResults.size() / REGION_SLOTS.length);
        int safePage = Math.min(page, Math.max(1, totalPages));

        int start = (safePage - 1) * REGION_SLOTS.length;
        int end = Math.min(start + REGION_SLOTS.length, searchResults.size());

        for (int i = start, slotIndex = 0; i < end && slotIndex < REGION_SLOTS.length; i++, slotIndex++) {
            OfflinePlayer player = searchResults.get(i);
            int slot = REGION_SLOTS[slotIndex];
            buildPlayerItem(player, slot);
        }

        buildFooter(searchResults.size(), totalPages);
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
        int totalRegions = plugin.getProtectionManager().getAllRegions().size();
        List<String> processedLore = new ArrayList<>();
        for (String line : searchHeaderLore) {
            processedLore.add(line
                    .replace("%search%", String.valueOf(searchResults.size()))
                    .replace("%found%", String.valueOf(searchResults.size()))
                    .replace("%total%", String.valueOf(totalRegions)));
        }
        ItemStack header = buildItem(searchHeaderMaterial, searchHeaderDisplayName, processedLore,
                searchHeaderCustomModelData, searchHeaderSkullValue);
        inventory.setItem(searchHeaderSlot, header);

        ItemStack separator = buildItem(separatorMaterial, separatorDisplayName, Collections.emptyList(),
                separatorCustomModelData, separatorSkullValue);
        inventory.setItem(3, separator);
        inventory.setItem(5, separator);
        inventory.setItem(2, separator.clone());
        inventory.setItem(6, separator.clone());
    }

    private void buildEmptyState() {
        List<String> processedLore = new ArrayList<>();
        for (String line : searchEmptyLore) {
            processedLore.add(line);
        }
        ItemStack empty = buildItem(searchEmptyMaterial, searchEmptyDisplayName, processedLore,
                searchEmptyCustomModelData, searchEmptySkullValue);
        inventory.setItem(22, empty);
    }

    private void buildPlayerItem(OfflinePlayer player, int slot) {
        UUID playerUuid = player.getUniqueId();
        long protections = plugin.getProtectionManager().getAllRegions().stream()
                .filter(r -> r.getOwnerUniqueId().equals(playerUuid))
                .count();

        List<String> processedLore = new ArrayList<>();
        for (String line : playerHeadLore) {
            processedLore.add(line
                    .replace("%player_name%", player.getName() != null ? player.getName() : playerUuid.toString())
                    .replace("%player_uuid%", playerUuid.toString())
                    .replace("%protections%", String.valueOf(protections)));
        }

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            item.setItemMeta(skullMeta);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(playerHeadDisplayName
                    .replace("%player_name%", player.getName() != null ? player.getName() : playerUuid.toString())
                    .replace("%player_uuid%", playerUuid.toString())
                    .replace("%protections%", String.valueOf(protections))));
            List<net.kyori.adventure.text.Component> components = new ArrayList<>();
            for (String line : processedLore) {
                components.add(mm.deserialize(line));
            }
            meta.lore(components);
            if (playerHeadCustomModelData != -1)
                meta.setCustomModelData(playerHeadCustomModelData);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        slotToPlayer.put(slot, player);
    }

    private void buildFooter(int totalPlayers, int totalPages) {
        int showing = Math.min(totalPlayers, REGION_SLOTS.length);

        List<String> processedPageLore = new ArrayList<>();
        for (String line : pageInfoLore) {
            processedPageLore.add(line
                    .replace("%current%", String.valueOf(page))
                    .replace("%total%", String.valueOf(totalPages))
                    .replace("%showing%", String.valueOf(showing))
                    .replace("%total_players%", String.valueOf(totalPlayers)));
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
        ItemStack item;
        if (material == Material.PLAYER_HEAD && skullValue != null && !skullValue.isEmpty()) {
            item = SkullUtils.createSkullWithTexture(skullValue);
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
        handleMenuClick(slot, player, org.bukkit.event.inventory.ClickType.LEFT);
    }

    @Override
    public void handleMenuClick(int slot, Player player, org.bukkit.event.inventory.ClickType clickType) {
        if (player == null || player.getLocation() == null)
            return;

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
            plugin.getMenuManager().openMenu(player, new AdminMenu(plugin, player, 1));
            return;
        }

        if (slot == refreshSlot) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new SearchMenu(plugin, searchResults, page));
            return;
        }

        if (slot == prevSlot && page > 1) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new SearchMenu(plugin, searchResults, page - 1));
            return;
        }

        if (slot == nextSlot) {
            int totalPages = (int) Math.ceil((double) searchResults.size() / REGION_SLOTS.length);
            if (page < totalPages) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openMenu(player, new SearchMenu(plugin, searchResults, page + 1));
            }
            return;
        }

        if (slotToPlayer.containsKey(slot)) {
            OfflinePlayer selectedPlayer = slotToPlayer.get(slot);
            if (selectedPlayer.isOnline() && selectedPlayer.getPlayer() != null) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
                plugin.getMenuManager().openMenu(player,
                        new PlayerProtectionListMenu(plugin, selectedPlayer.getPlayer(), 1));
            } else {
                player.sendMessage(mm.deserialize(MessageUtils.lang("search-menu.player-not-online")));
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
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
