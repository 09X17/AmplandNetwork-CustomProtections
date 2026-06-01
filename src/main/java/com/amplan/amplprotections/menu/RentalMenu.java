package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.model.Rental;
import com.amplan.amplprotections.utils.ItemBuilder;
import com.amplan.amplprotections.utils.MessageUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class RentalMenu implements MenuManager.CustomMenu {

    private final AmplProtections plugin;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final ProtectionRegion region;

    private final String menuTitle;
    private final Material borderMaterial;
    private final String borderName;
    private final int borderCustomModelData;
    private final int closeSlot;
    private final Material closeMaterial;
    private final String closeDisplayName;
    private final List<String> closeLore;
    private final int closeCustomModelData;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public RentalMenu(AmplProtections plugin, ProtectionRegion region) {
        this.plugin = plugin;
        this.region = region;

        FileConfiguration config = plugin.getMenuConfigManager().getRentalMenu();
        ConfigurationSection menuConfig = config;

        this.menuTitle = menuConfig != null
                ? menuConfig.getString("title", MessageUtils.lang("rental-menu.title"))
                : MessageUtils.lang("rental-menu.title");
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
        fillBorder();
        buildRentalInfo();
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

    private void buildRentalInfo() {
        Rental rental = plugin.getRentalManager().getRental(region);

        if (rental != null) {
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtils.lang("rental-menu.status-rented"));
            lore.add("<gray>Inquilino: <yellow>" + Bukkit.getOfflinePlayer(rental.getRenterUuid()).getName());
            lore.add("<gray>Precio: <yellow>$" + String.format("%.2f", rental.getPrice()));
            lore.add("<gray>Expira: <yellow>" + formatTime(rental.getRemainingSeconds()));
            lore.add(MessageUtils.lang("rental-menu.auto-renew-label").replace("%state%", rental.isAutoRenew() ? MessageUtils.lang("rental-menu.auto-renew-yes") : MessageUtils.lang("rental-menu.auto-renew-no")));
            lore.add("");
            lore.add("<red>Clic para cancelar alquiler");

            ItemStack infoItem = new ItemBuilder(Material.EMERALD)
                    .setDisplayName("<green><b>INFORMACION DEL ALQUILER</b></green>")
                    .setLore(lore.toArray(String[]::new))
                    .build();
            inventory.setItem(22, infoItem);
        } else {
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtils.lang("rental-menu.status-available"));
            lore.add("");
            lore.add("<green>Clic izq: Alquilar");
            lore.add("<yellow>Shift + Clic: Configurar alquiler");

            ItemStack infoItem = new ItemBuilder(Material.GOLD_INGOT)
                    .setDisplayName("<gold><b>PROTECCION DISPONIBLE</b></gold>")
                    .setLore(lore.toArray(String[]::new))
                    .build();
            inventory.setItem(22, infoItem);
        }
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

        if (slot == 22) {
            Rental rental = plugin.getRentalManager().getRental(region);
            if (rental != null) {
                if (rental.getRenterUuid().equals(player.getUniqueId())
                        || region.getOwnerUniqueId().equals(player.getUniqueId())
                        || player.hasPermission("amplprotections.admin.bypass")) {
                    plugin.getRentalManager().cancelRental(region, player);
                    Location loc = player.getLocation();
                    if (loc == null)
                        return;
                    player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                    player.closeInventory();
                }
            } else {
                plugin.getRentalManager().acceptRental(region, player);
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                player.closeInventory();
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private String formatTime(long seconds) {
        if (seconds <= 0)
            return MessageUtils.lang("time-expired");
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        if (days > 0)
            return days + "d " + hours + "h";
        if (hours > 0)
            return hours + "h " + mins + "m";
        return mins + "m";
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
