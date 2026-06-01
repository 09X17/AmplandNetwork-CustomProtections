package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.ItemBuilder;
import com.amplan.amplprotections.utils.MessageUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class RollbackMenu implements MenuManager.CustomMenu {

    private static final int[] PLAYER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final AmplProtections plugin;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final ProtectionRegion region;
    private final Map<Integer, UUID> slotToPlayer = new HashMap<>();

    private final String menuTitle;
    private final Material borderMaterial;
    private final String borderName;
    private final int borderCustomModelData;
    private final int closeSlot;
    private final Material closeMaterial;
    private final String closeDisplayName;
    private final List<String> closeLore;
    private final int closeCustomModelData;

    private final long maxAgeMillis;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public RollbackMenu(AmplProtections plugin, ProtectionRegion region) {
        this.plugin = plugin;
        this.region = region;

        int maxAgeDays = plugin.getConfig().getInt("rollback.max-age-days", 7);
        this.maxAgeMillis = maxAgeDays * 24L * 60L * 60L * 1000L;

        FileConfiguration config = plugin.getMenuConfigManager().getRollbackMenu();
        ConfigurationSection menuConfig = config;

        this.menuTitle = menuConfig != null
                ? menuConfig.getString("title", MessageUtils.lang("rollback-menu.title"))
                : MessageUtils.lang("rollback-menu.title");
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(menuTitle));

        ConfigurationSection borderCfg = menuConfig != null ? menuConfig.getConfigurationSection("border") : null;
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);

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
        slotToPlayer.clear();
        fillBorder();
        buildPlayerItems();
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

    private void buildPlayerItems() {
        plugin.getProtectionManager().getProtectionDao().getPlayersWithChangesAsync(region.getDatabaseId())
                .thenAccept(playerUuids -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        int slotIndex = 0;
                        for (UUID uuid : playerUuids) {
                            if (slotIndex >= PLAYER_SLOTS.length)
                                break;

                            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                            if (playerName == null)
                                playerName = uuid.toString().substring(0, 8);

                            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                            if (skullMeta != null) {
                                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                                skullMeta.displayName(net.kyori.adventure.text.Component.text(playerName));
                                head.setItemMeta(skullMeta);
                            }

                            ItemMeta meta = head.getItemMeta();
                            if (meta != null) {
                                List<String> lore = new ArrayList<>();
                                lore.add(MessageUtils.lang("rollback-menu.player-lore").replace("%player%", playerName));
                                lore.add(MessageUtils.lang("rollback-menu.player-hint"));
                                meta.lore(lore.stream().map(mm::deserialize).collect(Collectors.toList()));
                                head.setItemMeta(meta);
                            }

                            int slot = PLAYER_SLOTS[slotIndex];
                            inventory.setItem(slot, head);
                            slotToPlayer.put(slot, uuid);
                            slotIndex++;
                        }
                    });
                });
    }

    private void buildFooter() {
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

        ItemStack allBtn = new ItemBuilder(Material.TNT)
                .setDisplayName(MessageUtils.lang("rollback-menu.all-btn"))
                .setLore(MessageUtils.lang("rollback-menu.all-lore"))
                .build();
        inventory.setItem(47, allBtn);
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

        if (slot == 47) {
            if (player.isSneaking()) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                player.closeInventory();
                long since = System.currentTimeMillis() - maxAgeMillis;
                plugin.getRollbackManager().executeRollback(region, null, since, player);
            }
            return;
        }

        if (slotToPlayer.containsKey(slot)) {
            UUID targetUuid = slotToPlayer.get(slot);
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            player.closeInventory();

            long since = System.currentTimeMillis() - maxAgeMillis;
            plugin.getRollbackManager().executeRollback(region, targetUuid, since, player);
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
