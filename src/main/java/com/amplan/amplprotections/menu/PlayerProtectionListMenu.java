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
import org.bukkit.World;
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
import com.amplan.amplprotections.utils.MessageUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class PlayerProtectionListMenu implements MenuManager.CustomMenu {

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

    private final String regionItemDisplayName;
    private final List<String> regionItemLore;
    private final int regionItemCustomModelData;

    public PlayerProtectionListMenu(AmplProtections plugin, Player targetPlayer) {
        this(plugin, targetPlayer, 1);
    }

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public PlayerProtectionListMenu(AmplProtections plugin, Player targetPlayer, int page) {
        this.plugin = plugin;
        this.manager = plugin.getProtectionManager();
        this.page = Math.max(1, page);
        this.targetPlayer = targetPlayer;

        org.bukkit.configuration.file.FileConfiguration menuConfig = plugin.getMenuConfigManager().getListMenu();

        this.menuTitle = menuConfig != null ? menuConfig.getString("title", "<dark_gray>Mis Protecciones") : "<dark_gray>Mis Protecciones";
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(menuTitle));

        ConfigurationSection borderCfg = menuConfig != null ? menuConfig.getConfigurationSection("border") : null;
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);

        ConfigurationSection statsCfg = menuConfig != null ? menuConfig.getConfigurationSection("stats-item") : null;
        this.statsMaterial = getMaterial(statsCfg, "material", Material.BEACON);
        this.statsDisplayName = getConfigString(statsCfg, "display-name", "<gold><b>Estadisticas</b></gold>");
        this.statsLore = getConfigList(statsCfg, "lore", Arrays.asList(
                "<gray>Tus protecciones: <white>%player%",
                "",
                "<green>Clic para teletransportarte",
                "<yellow>Shift + Clic para administrar"
        ));
        this.statsCustomModelData = getCustomModelData(statsCfg, "custom-model-data", -1);

        ConfigurationSection emptyCfg = menuConfig != null ? menuConfig.getConfigurationSection("empty-item") : null;
        this.emptyMaterial = getMaterial(emptyCfg, "material", Material.BARRIER);
        this.emptyDisplayName = getConfigString(emptyCfg, "display-name", "<red><b>Sin Protecciones</b></red>");
        this.emptyLore = getConfigList(emptyCfg, "lore", Arrays.asList(
                "<gray>No tienes protecciones registradas.",
                "",
                "<yellow>Coloca un bloque de proteccion",
                "<yellow>para crear una zona"
        ));
        this.emptyCustomModelData = getCustomModelData(emptyCfg, "custom-model-data", -1);

        ConfigurationSection closeCfg = menuConfig != null ? menuConfig.getConfigurationSection("close-button") : null;
        this.closeSlot = closeCfg != null ? closeCfg.getInt("slot", 49) : 49;
        this.closeMaterial = getMaterial(closeCfg, "material", Material.BARRIER);
        this.closeDisplayName = getConfigString(closeCfg, "display-name", "<red><b>Cerrar</b></red>");
        this.closeLore = getConfigList(closeCfg, "lore", Collections.singletonList("<gray>Clic para salir"));
        this.closeCustomModelData = getCustomModelData(closeCfg, "custom-model-data", -1);

        ConfigurationSection refreshCfg = menuConfig != null ? menuConfig.getConfigurationSection("refresh-button") : null;
        this.refreshSlot = refreshCfg != null ? refreshCfg.getInt("slot", 51) : 51;
        this.refreshMaterial = getMaterial(refreshCfg, "material", Material.COMPASS);
        this.refreshDisplayName = getConfigString(refreshCfg, "display-name", "<aqua><b>Actualizar</b></aqua>");
        this.refreshLore = getConfigList(refreshCfg, "lore", Collections.singletonList("<gray>Clic para refrescar la lista"));
        this.refreshCustomModelData = getCustomModelData(refreshCfg, "custom-model-data", -1);

        ConfigurationSection pageCfg = menuConfig != null ? menuConfig.getConfigurationSection("page-info") : null;
        this.pageInfoSlot = pageCfg != null ? pageCfg.getInt("slot", 47) : 47;
        this.pageInfoMaterial = getMaterial(pageCfg, "material", Material.KNOWLEDGE_BOOK);
        this.pageInfoDisplayName = getConfigString(pageCfg, "display-name", "<white><b>Pagina %current% / %total%</b></white>");
        this.pageInfoLore = getConfigList(pageCfg, "lore", Arrays.asList(
                "<gray>Mostrando: <white>%showing% / %totalRegions% regiones"
        ));
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

        ConfigurationSection regionCfg = menuConfig != null ? menuConfig.getConfigurationSection("region-item") : null;
        this.regionItemDisplayName = getConfigString(regionCfg, "display-name", "<gold><b>%name%</b></gold>");
        this.regionItemLore = getConfigList(regionCfg, "lore", Arrays.asList(
                "<gray>ID: <yellow>%land_id%",
                "<gray>Mundo: <green>%world%",
                "<gray>Centro: <aqua>%x%, %y%, %z%",
                "<gray>Tamano: <yellow>%size%",
                "",
                "<dark_gray>Flags: <green>%enabled_flags%<dark_gray>/<gray>%total_flags%",
                "<gray>Miembros: <white>%members%",
                "",
                "<green>Clic izq: Teletransportarte",
                "<yellow>Shift + Clic: Administrar"
        ));
        this.regionItemCustomModelData = getCustomModelData(regionCfg, "custom-model-data", -1);

        buildInventoryContents();
    }

    private void buildInventoryContents() {
        inventory.clear();
        slotToRegionMap.clear();
        fillBorder();
        buildHeader();

        List<ProtectionRegion> regions = new ArrayList<>(manager.getRegionsByOwner(targetPlayer.getUniqueId()));

        if (regions.isEmpty()) {
            buildEmptyState();
            buildFooter(regions.size(), 1);
            return;
        }

        int totalPages = (int) Math.ceil((double) regions.size() / ITEMS_PER_PAGE);
        if (page > totalPages) {
            plugin.getMenuManager().openMenu(targetPlayer, new PlayerProtectionListMenu(plugin, targetPlayer, totalPages));
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
        int playerRegions = manager.getRegionsByOwner(targetPlayer.getUniqueId()).size();

        List<String> processedLore = new ArrayList<>();
        for (String line : statsLore) {
            processedLore.add(line.replace("%player%", String.valueOf(playerRegions)));
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

            int radius = region.getRadius();
            String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);
            long enabledFlags = region.getFlags().values().stream().filter(l -> l != com.amplan.amplprotections.model.FlagPermissionLevel.NONE).count();
            int totalFlags = region.getFlags().size();

            String displayName = regionItemDisplayName
                    .replace("%name%", region.getCustomName())
                    .replace("%land_id%", region.getLandId())
                    .replace("%world%", region.getWorldName())
                    .replace("%x%", String.valueOf(region.getBlockX()))
                    .replace("%y%", String.valueOf(region.getBlockY()))
                    .replace("%z%", String.valueOf(region.getBlockZ()))
                    .replace("%size%", size)
                    .replace("%enabled_flags%", String.valueOf(enabledFlags))
                    .replace("%total_flags%", String.valueOf(totalFlags))
                    .replace("%members%", String.valueOf(region.getMembers().size()));

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
                        .replace("%enabled_flags%", String.valueOf(enabledFlags))
                        .replace("%total_flags%", String.valueOf(totalFlags))
                        .replace("%members%", String.valueOf(region.getMembers().size())));
            }

            ItemStack regionItem;
            if (material == Material.PLAYER_HEAD && skullValue != null && !skullValue.isEmpty()) {
                regionItem = com.amplan.amplprotections.utils.SkullUtils.createSkullWithTexture(skullValue);
            } else {
                regionItem = new ItemStack(material);
            }

            ItemMeta meta = regionItem.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(displayName));
                List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line));
                }
                meta.lore(loreComponents);
                if (customModelData != -1) {
                    meta.setCustomModelData(customModelData);
                }
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
                    .replace("%totalRegions%", String.valueOf(totalRegions))
                    .replace("%total_regions%", String.valueOf(totalRegions)));
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
        handleMenuClick(slot, player, org.bukkit.event.inventory.ClickType.LEFT);
    }

    @Override
    public void handleMenuClick(int slot, Player player, org.bukkit.event.inventory.ClickType clickType) {
        if (slot == closeSlot) {
            Location loc = player.getLocation();
            if (loc == null) return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            return;
        }

        if (slot == refreshSlot) {
            Location loc = player.getLocation();
            if (loc == null) return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new PlayerProtectionListMenu(plugin, player, page));
            return;
        }

        if (slot == prevSlot && page > 1) {
            Location loc = player.getLocation();
            if (loc == null) return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new PlayerProtectionListMenu(plugin, player, page - 1));
            return;
        }

        if (slot == nextSlot) {
            List<ProtectionRegion> regions = manager.getRegionsByOwner(player.getUniqueId());
            int totalPages = (int) Math.ceil((double) regions.size() / ITEMS_PER_PAGE);
            if (page < totalPages) {
                Location loc = player.getLocation();
                if (loc == null) return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openMenu(player, new PlayerProtectionListMenu(plugin, player, page + 1));
            }
            return;
        }

        if (slotToRegionMap.containsKey(slot)) {
            ProtectionRegion selectedRegion = slotToRegionMap.get(slot);

            if (clickType == org.bukkit.event.inventory.ClickType.SHIFT_LEFT || clickType == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
                Location loc = player.getLocation();
                if (loc == null) return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
                plugin.getMenuManager().openMenu(player, new MainProtectionMenu(plugin, selectedRegion));
                return;
            }

            World world = Bukkit.getWorld(selectedRegion.getWorldName());
            if (world == null) {
                player.sendMessage(mm.deserialize("<red>✘ El mundo de esta protección no existe."));
                Location loc = player.getLocation();
                if (loc == null) return;
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }

            int targetX = selectedRegion.getBlockX();
            int targetZ = selectedRegion.getBlockZ();
            int targetY = findSafeY(world, targetX, selectedRegion.getBlockY(), targetZ);

            if (targetY == -1) {
                player.sendMessage(mm.deserialize("<red>✘ No se encontro una posicion segura para teletransportarse."));
                Location loc = player.getLocation();
                if (loc == null) return;
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }

            if (plugin.getTeleportCooldownManager().isOnCooldown(player)) {
                long remaining = plugin.getTeleportCooldownManager().getRemainingSeconds(player);
                String msg = MessageUtils.lang("teleport.cooldown").replace("%seconds%", String.valueOf(remaining));
                player.sendMessage(mm.deserialize(msg));
                Location loc = player.getLocation();
                if (loc == null) return;
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }

            Location tpLocation = new Location(world, targetX + 0.5, targetY, targetZ + 0.5);
            player.teleport(tpLocation);
            plugin.getTeleportCooldownManager().setCooldown(player);
            Location loc = player.getLocation();
            if (loc == null) return;
            player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            String tpMsg = MessageUtils.lang("teleport.teleported").replace("%region%", selectedRegion.getCustomName());
            player.sendMessage(mm.deserialize(tpMsg));
            player.closeInventory();
        }
    }

    private int findSafeY(World world, int x, int preferredY, int z) {
        int startY = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 2, preferredY));

        for (int y = startY; y <= world.getMaxHeight() - 2; y++) {
            if (isSafeLocation(world, x, y, z)) {
                return y + 1;
            }
        }

        for (int y = startY - 1; y >= world.getMinHeight(); y--) {
            if (isSafeLocation(world, x, y, z)) {
                return y + 1;
            }
        }

        return -1;
    }

    private boolean isSafeLocation(World world, int x, int y, int z) {
        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();
        Material ground = world.getBlockAt(x, y - 1, z).getType();

        boolean feetSafe = !feet.isSolid() && feet != Material.LAVA && feet != Material.WATER;
        boolean headSafe = !head.isSolid() && head != Material.LAVA && head != Material.WATER;
        boolean groundSolid = ground.isSolid();

        return feetSafe && headSafe && groundSolid;
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
