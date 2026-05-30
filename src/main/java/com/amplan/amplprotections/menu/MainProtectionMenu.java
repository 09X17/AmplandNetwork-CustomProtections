package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.MessageUtils;
import com.amplan.amplprotections.utils.SkullUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class MainProtectionMenu implements MenuManager.CustomMenu {

    private static final int[] DEFAULT_FLAG_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final AmplProtections plugin;
    private final ProtectionRegion region;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<Integer, String> slotToFlag = new HashMap<>();
    private final List<String> allEnabledFlags;
    private final int page;
    private final int[] flagSlots;

    private final String menuTitle;
    private final boolean borderEnabled;
    private final Material borderMaterial;
    private final String borderName;
    private final int borderCustomModelData;
    private final String borderSkullValue;
    private final boolean separatorEnabled;
    private final Material separatorMaterial;
    private final String separatorName;
    private final int separatorCustomModelData;
    private final Material infoMaterial;
    private final boolean infoEnabled;
    private final int infoSlot;
    private final String infoDisplayName;
    private final List<String> infoLore;
    private final int infoCustomModelData;
    private final String infoSkullValue;
    private final Material statusEnabledMaterial;
    private final Material statusDisabledMaterial;
    private final boolean statusItemEnabled;
    private final int statusSlotLeft;
    private final int statusSlotRight;
    private final String statusDisplayName;
    private final List<String> statusLore;
    private final int statusCustomModelData;
    private final int membersSlot;
    private final boolean membersEnabled;
    private final Material membersMaterial;
    private final String membersDisplayName;
    private final List<String> membersLore;
    private final int membersCustomModelData;
    private final String membersSkullValue;
    private final int closeSlot;
    private final boolean closeEnabled;
    private final Material closeMaterial;
    private final String closeDisplayName;
    private final List<String> closeLore;
    private final int closeCustomModelData;
    private final String closeSkullValue;
    private final int deleteSlot;
    private final boolean deleteEnabled;
    private final Material deleteMaterial;
    private final String deleteDisplayName;
    private final List<String> deleteLore;
    private final int deleteCustomModelData;
    private final String deleteSkullValue;
    private final int viewSlot;
    private final boolean viewEnabled;
    private final Material viewMaterial;
    private final String viewDisplayName;
    private final List<String> viewLore;
    private final int viewCustomModelData;
    private final String viewSkullValue;
    private final int prevSlot;
    private final boolean prevEnabled;
    private final Material prevMaterial;
    private final String prevDisplayName;
    private final List<String> prevLore;
    private final int prevCustomModelData;
    private final int nextSlot;
    private final boolean nextEnabled;
    private final Material nextMaterial;
    private final String nextDisplayName;
    private final List<String> nextLore;
    private final int nextCustomModelData;
    private final Material pageInfoMaterial;
    private final boolean pageInfoEnabled;
    private final String pageInfoDisplayName;
    private final List<String> pageInfoLore;
    private final int pageInfoCustomModelData;
    private final boolean hologramEnabled;
    private final int hologramSlot;
    private final Material hologramMaterial;
    private final int hologramCustomModelData;
    private final String hologramDisplayNameOn;
    private final String hologramDisplayNameOff;
    private final List<String> hologramLoreOn;
    private final List<String> hologramLoreOff;
    private final String hologramSkullValue;
    private final boolean mergeEnabled;
    private final int mergeSlot;
    private final Material mergeMaterial;
    private final int mergeCustomModelData;
    private final String mergeDisplayName;
    private final List<String> mergeLore;
    private final String mergeSkullValue;
    private final boolean presetEnabled;
    private final int presetSlot;
    private final Material presetMaterial;
    private final int presetCustomModelData;
    private final String presetDisplayName;
    private final List<String> presetLore;
    private final String presetSkullValue;

    public MainProtectionMenu(AmplProtections plugin, ProtectionRegion region) {
        this(plugin, region, 1);
    }

    public MainProtectionMenu(AmplProtections plugin, ProtectionRegion region, int page) {
        this.plugin = plugin;
        this.region = region;
        this.page = Math.max(1, page);

        org.bukkit.configuration.file.FileConfiguration menuConfig = plugin.getMenuConfigManager().getMainMenu();
        List<String> configEnabledFlags = plugin.getConfig().getStringList("flags.enabled");
        org.bukkit.configuration.ConfigurationSection flagsMenuConfig = menuConfig.getConfigurationSection("flags");

        this.allEnabledFlags = configEnabledFlags.stream()
                .filter(f -> {
                    if (flagsMenuConfig == null)
                        return true;
                    ConfigurationSection fSection = flagsMenuConfig.getConfigurationSection(f);
                    if (fSection == null)
                        return true;
                    return fSection.getBoolean("enabled", true);
                })
                .collect(Collectors.toList());

        List<Integer> configuredSlots = menuConfig.getIntegerList("flag-slots");
        this.flagSlots = configuredSlots.isEmpty()
                ? DEFAULT_FLAG_SLOTS
                : configuredSlots.stream()
                        .mapToInt(Integer::intValue)
                        .toArray();

        this.menuTitle = getConfigString(menuConfig, "title", "<dark_gray>Ajustes de Proteccion");
        this.inventory = Bukkit.createInventory(null, 54, mm.deserialize(menuTitle));

        ConfigurationSection borderCfg = menuConfig.getConfigurationSection("border");
        this.borderEnabled = borderCfg != null ? borderCfg.getBoolean("enabled", true) : true;
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);
        this.borderSkullValue = getConfigString(borderCfg, "skull-value", null);

        ConfigurationSection sepCfg = menuConfig.getConfigurationSection("separator");
        this.separatorEnabled = sepCfg != null ? sepCfg.getBoolean("enabled", true) : true;
        this.separatorMaterial = getMaterial(sepCfg, "material", Material.GRAY_STAINED_GLASS_PANE);
        this.separatorName = getConfigString(sepCfg, "display-name", "<dark_gray> ");
        this.separatorCustomModelData = getCustomModelData(sepCfg, "custom-model-data", -1);

        ConfigurationSection infoCfg = menuConfig.getConfigurationSection("info-item");
        this.infoEnabled = infoCfg != null ? infoCfg.getBoolean("enabled", true) : true;
        this.infoSlot = infoCfg != null ? infoCfg.getInt("slot", 13) : 13;
        this.infoMaterial = getMaterial(infoCfg, "material", Material.KNOWLEDGE_BOOK);
        this.infoDisplayName = getConfigString(infoCfg, "display-name", "<gold><b>Informacion</b></gold>");
        this.infoLore = getConfigList(infoCfg, "lore", Arrays.asList(
                "<gray>Nombre: <white>%name%",
                "<gray>Tipo: <white>%type%",
                "<gray>Tamano: <yellow>%size%",
                "<gray>Centro: <aqua>X:%x% Y:%y% Z:%z%"));
        this.infoCustomModelData = getCustomModelData(infoCfg, "custom-model-data", -1);
        this.infoSkullValue = getConfigString(infoCfg, "skull-value", null);

        ConfigurationSection statusCfg = menuConfig.getConfigurationSection("status-item");
        this.statusItemEnabled = statusCfg != null ? statusCfg.getBoolean("enabled", true) : true;
        this.statusSlotLeft = statusCfg != null ? statusCfg.getInt("slot-left", 10) : 10;
        this.statusSlotRight = statusCfg != null ? statusCfg.getInt("slot-right", 16) : 16;
        this.statusEnabledMaterial = getMaterial(statusCfg, "enabled-material", Material.LIME_DYE);
        this.statusDisabledMaterial = getMaterial(statusCfg, "disabled-material", Material.RED_DYE);
        this.statusDisplayName = getConfigString(statusCfg, "display-name", "<white><b>Estado</b></white>");
        this.statusLore = getConfigList(statusCfg, "lore", Arrays.asList(
                "<green>Permitidas: <white>%enabled%",
                "<red>Denegadas: <white>%disabled%"));
        this.statusCustomModelData = getCustomModelData(statusCfg, "custom-model-data", -1);

        ConfigurationSection membersCfg = menuConfig.getConfigurationSection("members-button");
        this.membersEnabled = membersCfg != null ? membersCfg.getBoolean("enabled", true) : true;
        this.membersSlot = membersCfg != null ? membersCfg.getInt("slot", 47) : 47;
        this.membersMaterial = getMaterial(membersCfg, "material", Material.PLAYER_HEAD);
        this.membersDisplayName = getConfigString(membersCfg, "display-name", "<yellow><b>Miembros</b></yellow>");
        this.membersLore = getConfigList(membersCfg, "lore", Arrays.asList(
                "<gray>Actuales: <white>%count%", "", "<green>Clic para gestionar"));
        this.membersCustomModelData = getCustomModelData(membersCfg, "custom-model-data", -1);
        this.membersSkullValue = getConfigString(membersCfg, "skull-value", null);

        ConfigurationSection closeCfg = menuConfig.getConfigurationSection("close-button");
        this.closeEnabled = closeCfg != null ? closeCfg.getBoolean("enabled", true) : true;
        this.closeSlot = closeCfg != null ? closeCfg.getInt("slot", 49) : 49;
        this.closeMaterial = getMaterial(closeCfg, "material", Material.BARRIER);
        this.closeDisplayName = getConfigString(closeCfg, "display-name", "<red><b>Cerrar</b></red>");
        this.closeLore = getConfigList(closeCfg, "lore", Collections.singletonList("<gray>Clic para salir"));
        this.closeCustomModelData = getCustomModelData(closeCfg, "custom-model-data", -1);
        this.closeSkullValue = getConfigString(closeCfg, "skull-value", null);

        ConfigurationSection deleteCfg = menuConfig.getConfigurationSection("delete-button");
        this.deleteEnabled = deleteCfg != null ? deleteCfg.getBoolean("enabled", true) : true;
        this.deleteSlot = deleteCfg != null ? deleteCfg.getInt("slot", 51) : 51;
        this.deleteMaterial = getMaterial(deleteCfg, "material", Material.TNT);
        this.deleteDisplayName = getConfigString(deleteCfg, "display-name", "<red><b>Eliminar</b></red>");
        this.deleteLore = getConfigList(deleteCfg, "lore", Arrays.asList(
                "<red>Accion irreversible!", "", "<dark_gray>Shift + Clic para eliminar"));
        this.deleteCustomModelData = getCustomModelData(deleteCfg, "custom-model-data", -1);
        this.deleteSkullValue = getConfigString(deleteCfg, "skull-value", null);

        ConfigurationSection viewCfg = menuConfig.getConfigurationSection("view-button");
        this.viewEnabled = viewCfg != null ? viewCfg.getBoolean("enabled", true) : true;
        this.viewSlot = viewCfg != null ? viewCfg.getInt("slot", 45) : 45;
        this.viewMaterial = getMaterial(viewCfg, "material", Material.COMPASS);
        this.viewDisplayName = getConfigString(viewCfg, "display-name", "<aqua><b>Ver Limites</b></aqua>");
        this.viewLore = getConfigList(viewCfg, "lore",
                Collections.singletonList("<gray>Clic para preview con particulas"));
        this.viewCustomModelData = getCustomModelData(viewCfg, "custom-model-data", -1);
        this.viewSkullValue = getConfigString(viewCfg, "skull-value", null);

        ConfigurationSection prevCfg = menuConfig.getConfigurationSection("prev-page-button");
        this.prevEnabled = prevCfg != null ? prevCfg.getBoolean("enabled", true) : true;
        this.prevSlot = prevCfg != null ? prevCfg.getInt("slot", 46) : 46;
        this.prevMaterial = getMaterial(prevCfg, "material", Material.ARROW);
        this.prevDisplayName = getConfigString(prevCfg, "display-name", "<green><b>Anterior</b></green>");
        this.prevLore = getConfigList(prevCfg, "lore", Collections.singletonList("<gray>Clic para pagina anterior"));
        this.prevCustomModelData = getCustomModelData(prevCfg, "custom-model-data", -1);

        ConfigurationSection nextCfg = menuConfig.getConfigurationSection("next-page-button");
        this.nextEnabled = nextCfg != null ? nextCfg.getBoolean("enabled", true) : true;
        this.nextSlot = nextCfg != null ? nextCfg.getInt("slot", 52) : 52;
        this.nextMaterial = getMaterial(nextCfg, "material", Material.ARROW);
        this.nextDisplayName = getConfigString(nextCfg, "display-name", "<green><b>Siguiente</b></green>");
        this.nextLore = getConfigList(nextCfg, "lore", Collections.singletonList("<gray>Clic para pagina siguiente"));
        this.nextCustomModelData = getCustomModelData(nextCfg, "custom-model-data", -1);

        ConfigurationSection pageInfoCfg = menuConfig.getConfigurationSection("page-info");
        this.pageInfoEnabled = pageInfoCfg != null ? pageInfoCfg.getBoolean("enabled", true) : true;
        this.pageInfoMaterial = getMaterial(pageInfoCfg, "material", Material.KNOWLEDGE_BOOK);
        this.pageInfoDisplayName = getConfigString(pageInfoCfg, "display-name",
                "<white>Pagina %current% / %total%</white>");
        this.pageInfoLore = getConfigList(pageInfoCfg, "lore",
                Collections.singletonList("<gray>Flags: <white>%showing%"));
        this.pageInfoCustomModelData = getCustomModelData(pageInfoCfg, "custom-model-data", -1);

        ConfigurationSection holoCfg = menuConfig.getConfigurationSection("hologram-button");
        this.hologramEnabled = holoCfg != null ? holoCfg.getBoolean("enabled", true) : true;
        this.hologramSlot = holoCfg != null ? holoCfg.getInt("slot", 48) : 48;
        this.hologramMaterial = getMaterial(holoCfg, "material", Material.END_CRYSTAL);
        this.hologramCustomModelData = getCustomModelData(holoCfg, "custom-model-data", -1);
        this.hologramDisplayNameOn = getConfigString(holoCfg, "display-name-on", "<green><b>Holograma: ON</b></green>");
        this.hologramDisplayNameOff = getConfigString(holoCfg, "display-name-off", "<red><b>Holograma: OFF</b></red>");
        this.hologramLoreOn = getConfigList(holoCfg, "lore-on",
                Collections.singletonList("<gray>Clic para desactivar"));
        this.hologramLoreOff = getConfigList(holoCfg, "lore-off", Collections.singletonList("<gray>Clic para activar"));
        this.hologramSkullValue = getConfigString(holoCfg, "skull-value", null);

        ConfigurationSection mergeCfg = menuConfig.getConfigurationSection("merge-button");
        this.mergeEnabled = mergeCfg != null ? mergeCfg.getBoolean("enabled", true) : true;
        this.mergeSlot = mergeCfg != null ? mergeCfg.getInt("slot", 50) : 50;
        this.mergeMaterial = getMaterial(mergeCfg, "material", Material.ANVIL);
        this.mergeCustomModelData = getCustomModelData(mergeCfg, "custom-model-data", -1);
        this.mergeDisplayName = getConfigString(mergeCfg, "display-name", "<gold><b>Fusionar</b></gold>");
        this.mergeLore = getConfigList(mergeCfg, "lore",
                Collections.singletonList("<gray>Clic para fusionar con otra proteccion"));
        this.mergeSkullValue = getConfigString(mergeCfg, "skull-value", null);

        ConfigurationSection presetCfg = menuConfig.getConfigurationSection("preset-button");
        this.presetEnabled = presetCfg != null ? presetCfg.getBoolean("enabled", true) : true;
        this.presetSlot = presetCfg != null ? presetCfg.getInt("slot", 44) : 44;
        this.presetMaterial = getMaterial(presetCfg, "material", Material.BOOK);
        this.presetCustomModelData = getCustomModelData(presetCfg, "custom-model-data", -1);
        this.presetDisplayName = getConfigString(presetCfg, "display-name", "<aqua><b>Presets</b></aqua>");
        this.presetLore = getConfigList(presetCfg, "lore",
                Collections.singletonList("<gray>Clic para aplicar un preset de flags"));
        this.presetSkullValue = getConfigString(presetCfg, "skull-value", null);

        buildInventoryContents();
    }

    private void buildInventoryContents() {
        inventory.clear();
        slotToFlag.clear();
        fillBorder();
        buildHeaderBar();
        buildFlagPagination();
        buildFooterBar();
        buildPaginationBar();
    }

    private void fillBorder() {
        if (!borderEnabled)
            return;
        ItemStack border = buildConfigurableItem(borderMaterial, borderName, Collections.emptyList(),
                borderCustomModelData, borderSkullValue);
        for (int i = 0; i < 9; i++)
            inventory.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, border);
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    private void buildHeaderBar() {
        int radius = region.getRadius();
        String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);

        List<String> processedLore = new ArrayList<>(infoLore);
        String customLore = region.getCustomLore();
        if (customLore != null && !customLore.isEmpty()) {
            processedLore.add("");
            processedLore.add("<gray>─────────────────");
            processedLore.add("<gray>" + customLore);
            processedLore.add("<gray>─────────────────");
        }

        String[] processedInfoLore = processedLore.stream()
                .map(line -> line.replace("%name%", region.getCustomName())
                        .replace("%land%", region.getLandId())
                        .replace("%type%", region.getTypeId().replace("_", " ").toUpperCase())
                        .replace("%size%", size)
                        .replace("%x%", String.valueOf(region.getBlockX()))
                        .replace("%y%", String.valueOf(region.getBlockY()))
                        .replace("%z%", String.valueOf(region.getBlockZ())))
                .toArray(String[]::new);

        if (infoEnabled) {
            ItemStack infoItem = buildConfigurableItem(infoMaterial, infoDisplayName, Arrays.asList(processedInfoLore),
                    infoCustomModelData, infoSkullValue);
            inventory.setItem(infoSlot, infoItem);
        }

        int enabledCount = (int) allEnabledFlags.stream().filter(region::isFlagEnabled).count();
        int disabledCount = allEnabledFlags.size() - enabledCount;
        Material statusMat = enabledCount > disabledCount ? statusEnabledMaterial : statusDisabledMaterial;

        String[] processedStatusLore = statusLore.stream()
                .map(line -> line.replace("%enabled%", String.valueOf(enabledCount))
                        .replace("%disabled%", String.valueOf(disabledCount)))
                .toArray(String[]::new);

        if (statusItemEnabled) {
            ItemStack statusItem = buildConfigurableItem(statusMat, statusDisplayName,
                    Arrays.asList(processedStatusLore), statusCustomModelData, null);
            inventory.setItem(statusSlotLeft, statusItem);
            inventory.setItem(statusSlotRight, statusItem.clone());
        }

        if (separatorEnabled) {
            ItemStack separator = buildConfigurableItem(separatorMaterial, separatorName, Collections.emptyList(),
                    separatorCustomModelData, null);
            for (int i = 9; i < 18; i++) {
                if (inventory.getItem(i) == null)
                    inventory.setItem(i, separator);
            }
        }
    }

    private void buildFlagPagination() {
        org.bukkit.configuration.file.FileConfiguration menuConfig = plugin.getMenuConfigManager().getMainMenu();
        org.bukkit.configuration.ConfigurationSection flagsMenuConfig = menuConfig.getConfigurationSection("flags");
        int flagsPerPage = flagSlots.length;
        int totalPages = (int) Math.ceil((double) allEnabledFlags.size() / flagsPerPage);
        int safePage = Math.min(page, Math.max(1, totalPages));

        int start = (safePage - 1) * flagsPerPage;
        int end = Math.min(start + flagsPerPage, allEnabledFlags.size());

        for (int i = start, slotIndex = 0; i < end && slotIndex < flagSlots.length; slotIndex++) {
            String flag = allEnabledFlags.get(i);
            int slot = flagSlots[slotIndex];
            buildFlagItem(flag, slot, flagsMenuConfig);
            i++;
        }
    }

    private void buildFlagItem(String flag, int slot, ConfigurationSection menuConfig) {
        com.amplan.amplprotections.model.FlagPermissionLevel level = region.getFlagLevel(flag);
        String levelKey = level.name().toLowerCase();

        ConfigurationSection flagItemGlobal = plugin.getMenuConfigManager().getMainMenu()
                .getConfigurationSection("flag-item");

        String globalType = flagItemGlobal != null ? flagItemGlobal.getString("type", "custom-model-data")
                : "custom-model-data";

        Material globalMaterial = Material.PAPER;
        if (flagItemGlobal != null) {
            ConfigurationSection matSection = flagItemGlobal.getConfigurationSection("material");
            if (matSection != null) {
                String matName = matSection.getString(levelKey, null);
                if (matName != null) {
                    try {
                        globalMaterial = Material.valueOf(matName);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } else {
                globalMaterial = getMaterial(flagItemGlobal, "material", Material.PAPER);
            }
        }

        int globalCmd = flagItemGlobal != null ? flagItemGlobal.getInt("custom-model-data." + levelKey, -1) : -1;
        String globalSkull = flagItemGlobal != null
                ? getConfigString(flagItemGlobal.getConfigurationSection("skull-value"), levelKey, "")
                : "";

        Material material = globalMaterial;
        int customModelData = globalCmd;
        String skullValue = globalSkull;
        String displayName = null;
        List<String> loreLines = null;

        if (menuConfig != null) {
            ConfigurationSection flagSection = menuConfig.getConfigurationSection(flag);
            if (flagSection != null) {
                String matName = flagSection.getString("material");
                if (matName != null) {
                    try {
                        material = Material.valueOf(matName);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                String cmdPath = "custom-model-data-" + levelKey;
                int flagCmd = flagSection.getInt(cmdPath, -1);
                if (flagCmd != -1)
                    customModelData = flagCmd;
                String flagSkull = flagSection.getString("skull-value-" + levelKey, null);
                if (flagSkull != null)
                    skullValue = flagSkull;
                displayName = flagSection.getString("display-name");
                loreLines = flagSection.getStringList("lore");
            }
        }

        boolean useSkull = "skull".equals(globalType)
                || (material == Material.PLAYER_HEAD
                        && !skullValue.isEmpty());

        ItemStack item;
        if (useSkull && skullValue != null && !skullValue.isEmpty()) {
            item = SkullUtils.createSkullWithTexture(skullValue);
        } else {
            item = new ItemStack(material);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String levelColor = switch (level) {
                case NONE -> "<red>";
                case OWNER -> "<gold>";
                case MEMBERS -> "<green>";
                case ADMINS -> "<yellow>";
                case EVERYONE -> "<aqua>";
            };
            String levelDisplay = level.getDisplayName();
            String levelDesc = level.getDescription();

            if (displayName != null) {
                meta.displayName(mm.deserialize(displayName
                        .replace("%flag%", flag.toUpperCase())
                        .replace("%state%", levelDisplay)
                        .replace("%level%", levelDisplay)));
            } else {
                meta.displayName(mm.deserialize(levelColor + "<b>" + flag.toUpperCase()));
            }

            List<net.kyori.adventure.text.Component> lore;
            if (loreLines != null && !loreLines.isEmpty()) {
                lore = loreLines.stream()
                        .map(line -> mm.deserialize(line
                                .replace("%flag%", flag.toUpperCase())
                                .replace("%state%", levelDisplay)
                                .replace("%level%", levelDisplay)
                                .replace("%description%", levelDesc)))
                        .toList();
            } else {
                boolean isEnvironmental = com.amplan.amplprotections.model.FlagPermissionLevel.isEnvironmental(flag);
                String hint = isEnvironmental ? "<gray>Clic para activar/desactivar"
                        : "<gray>Clic izq: siguiente | Clic der: anterior";
                lore = List.of(
                        mm.deserialize("<gray>Permiso: " + levelColor + levelDisplay),
                        mm.deserialize(levelDesc),
                        mm.deserialize(hint));
            }

            meta.lore(lore);
            if (customModelData != -1)
                meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        slotToFlag.put(slot, flag);
    }

    private void buildFooterBar() {
        int memberCount = region.getMembers().size();

        String[] processedMembersLore = membersLore.stream()
                .map(line -> line.replace("%count%", String.valueOf(memberCount)))
                .toArray(String[]::new);

        if (membersEnabled) {
            ItemStack membersBtn = buildConfigurableItem(membersMaterial, membersDisplayName,
                    Arrays.asList(processedMembersLore), membersCustomModelData, membersSkullValue);
            inventory.setItem(membersSlot, membersBtn);
        }

        if (closeEnabled) {
            ItemStack closeBtn = buildConfigurableItem(closeMaterial, closeDisplayName, closeLore, closeCustomModelData,
                    closeSkullValue);
            inventory.setItem(closeSlot, closeBtn);
        }

        if (deleteEnabled) {
            ItemStack deleteBtn = buildConfigurableItem(deleteMaterial, deleteDisplayName, deleteLore,
                    deleteCustomModelData, deleteSkullValue);
            inventory.setItem(deleteSlot, deleteBtn);
        }

        if (viewEnabled) {
            ItemStack viewBtn = buildConfigurableItem(viewMaterial, viewDisplayName, viewLore, viewCustomModelData,
                    viewSkullValue);
            inventory.setItem(viewSlot, viewBtn);
        }

        if (hologramEnabled) {
            String holoName = region.isHologramEnabled() ? hologramDisplayNameOn : hologramDisplayNameOff;
            List<String> holoLore = region.isHologramEnabled() ? hologramLoreOn : hologramLoreOff;
            ItemStack holoBtn = buildConfigurableItem(hologramMaterial, holoName, holoLore, hologramCustomModelData,
                    hologramSkullValue);
            inventory.setItem(hologramSlot, holoBtn);
        }

        if (mergeEnabled) {
            ItemStack mergeBtn = buildConfigurableItem(mergeMaterial, mergeDisplayName, mergeLore, mergeCustomModelData,
                    mergeSkullValue);
            inventory.setItem(mergeSlot, mergeBtn);
        }

        if (presetEnabled) {
            ItemStack presetBtn = buildConfigurableItem(presetMaterial, presetDisplayName, presetLore,
                    presetCustomModelData, presetSkullValue);
            inventory.setItem(presetSlot, presetBtn);
        }
    }

    private void buildPaginationBar() {
        int flagsPerPage = flagSlots.length;
        int totalPages = (int) Math.ceil((double) allEnabledFlags.size() / flagsPerPage);
        if (totalPages <= 1)
            return;
        int safePage = Math.min(page, totalPages);

        String[] processedInfoLore = pageInfoLore.stream()
                .map(line -> line.replace("%current%", String.valueOf(safePage))
                        .replace("%total%", String.valueOf(totalPages))
                        .replace("%showing%", String.valueOf(allEnabledFlags.size())))
                .toArray(String[]::new);

        if (pageInfoEnabled) {
            ItemStack pageInfo = buildConfigurableItem(pageInfoMaterial,
                    pageInfoDisplayName.replace("%current%", String.valueOf(safePage)).replace("%total%",
                            String.valueOf(totalPages)),
                    Arrays.asList(processedInfoLore), pageInfoCustomModelData, null);
            inventory.setItem(49, pageInfo);
        }

        if (prevEnabled && safePage > 1) {
            String[] prevProcessedLore = prevLore.stream()
                    .map(line -> line.replace("%page%", String.valueOf(safePage - 1)))
                    .toArray(String[]::new);
            ItemStack prevBtn = buildConfigurableItem(prevMaterial, prevDisplayName, Arrays.asList(prevProcessedLore),
                    prevCustomModelData, null);
            inventory.setItem(prevSlot, prevBtn);
        }

        if (nextEnabled && safePage < totalPages) {
            String[] nextProcessedLore = nextLore.stream()
                    .map(line -> line.replace("%page%", String.valueOf(safePage + 1)))
                    .toArray(String[]::new);
            ItemStack nextBtn = buildConfigurableItem(nextMaterial, nextDisplayName, Arrays.asList(nextProcessedLore),
                    nextCustomModelData, null);
            inventory.setItem(nextSlot, nextBtn);
        }
    }

    private ItemStack buildConfigurableItem(Material material, String displayName, List<String> lore,
            int customModelData, String skullValue) {
        ItemStack item;
        if (material == Material.PLAYER_HEAD && skullValue != null && !skullValue.isEmpty()) {
            item = SkullUtils.createSkullWithTexture(skullValue);
        } else {
            item = new ItemStack(material);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.displayName(mm.deserialize(displayName));
            }
            if (lore != null && !lore.isEmpty()) {
                List<net.kyori.adventure.text.Component> components = lore.stream()
                        .map(mm::deserialize)
                        .collect(Collectors.toList());
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
        if (slot == closeSlot) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            return;
        }

        if (slot == membersSlot) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            MembersMenu membersMenu = new MembersMenu(plugin, region);
            membersMenu.setMenuItems();
            plugin.getMenuManager().openMenu(player, membersMenu);
            return;
        }

        if (slot == deleteSlot) {
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                plugin.getProtectionManager().removeRegion(region);
                player.sendMessage(mm.deserialize(msg("menu.protection-deleted")));
                player.closeInventory();
            } else {
                player.sendMessage(mm.deserialize(msg("menu.delete-shift-warning")));
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }
            return;
        }

        if (slot == viewSlot) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            com.amplan.amplprotections.utils.ParticleUtils.showProtectionView(plugin, player, region);
            player.sendMessage(mm.deserialize(msg("view.preview-start")));
            return;
        }

        if (slot == hologramSlot && hologramEnabled) {
            String togglePerm = java.util.Objects.requireNonNullElse(
                    plugin.getConfig().getString("holograms.toggle-permission"),
                    "amplprotections.hologram.toggle");
            if (!player.hasPermission(togglePerm)) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(mm.deserialize(MessageUtils.lang("hologram.no-permission")));
                return;
            }
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            boolean newState = !region.isHologramEnabled();
            region.setHologramEnabled(newState);
            plugin.getProtectionManager().getProtectionDao().saveHologramStateAsync(region.getDatabaseId(), newState);

            if (newState) {
                plugin.getHologramManager().spawnHologram(region);
                player.sendMessage(mm.deserialize(MessageUtils.lang("hologram.toggled-on")));
            } else {
                plugin.getHologramManager().removeHologram(region);
                player.sendMessage(mm.deserialize(MessageUtils.lang("hologram.toggled-off")));
            }
            buildInventoryContents();
            player.updateInventory();
            return;
        }

        if (slot == mergeSlot && mergeEnabled) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new MergeMenu(plugin, region));
            return;
        }

        if (slot == presetSlot && presetEnabled) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new PresetMenu(plugin, region, player));
            return;
        }

        if (slot == prevSlot) {
            if (page > 1) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openMenu(player, new MainProtectionMenu(plugin, region, page - 1));
            }
            return;
        }

        if (slot == nextSlot) {
            int flagsPerPage = flagSlots.length;
            int totalPages = (int) Math.ceil((double) allEnabledFlags.size() / flagsPerPage);
            if (page < totalPages) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openMenu(player, new MainProtectionMenu(plugin, region, page + 1));
            }
            return;
        }

        if (slotToFlag.containsKey(slot)) {
            String flag = slotToFlag.get(slot);
            com.amplan.amplprotections.model.FlagPermissionLevel currentLevel = region.getFlagLevel(flag);
            com.amplan.amplprotections.model.FlagPermissionLevel newLevel;
            boolean isEnvironmental = com.amplan.amplprotections.model.FlagPermissionLevel.isEnvironmental(flag);

            if (isEnvironmental) {
                if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
                    newLevel = currentLevel.previousSimple();
                } else {
                    newLevel = currentLevel.nextSimple();
                }
            } else {
                if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
                    newLevel = currentLevel.previous();
                } else {
                    newLevel = currentLevel.next();
                }
            }

            region.setFlagLevel(flag, newLevel);
            plugin.getProtectionManager().getProtectionDao().saveFlagAsync(region.getDatabaseId(), flag, newLevel);

            String levelMsg = switch (newLevel) {
                case NONE -> "<red>Nadie";
                case OWNER -> "<gold>Solo dueño";
                case MEMBERS -> "<green>Solo miembros";
                case ADMINS -> "<yellow>Solo admins+";
                case EVERYONE -> "<aqua>Todos";
            };
            String modeHint = isEnvironmental ? "<gray>(simple)" : "";
            String flagMsg = MessageUtils.lang("commands.flag-changed")
                    .replace("%flag%", flag.toUpperCase())
                    .replace("%level%", levelMsg)
                    .replace("%mode%", modeHint);
            player.sendMessage(mm.deserialize(flagMsg));

            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            buildInventoryContents();
            player.updateInventory();
        }
    }

    public List<String> getAllEnabledFlags() {
        return allEnabledFlags;
    }

    private String msg(String key) {
        return MessageUtils.lang(key);
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

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
