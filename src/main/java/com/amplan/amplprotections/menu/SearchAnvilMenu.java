package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.amplan.amplprotections.AmplProtections;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class SearchAnvilMenu implements Listener {

    private static final Set<UUID> searchingPlayers = new HashSet<>();

    private final AmplProtections plugin;
    private final Player player;
    private final Inventory anvilInventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    private final String title;
    private final String defaultText;
    private final String emptySearchMsg;
    private final String playerNotFoundMsg;

    private SearchAnvilMenu(AmplProtections plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        FileConfiguration config = plugin.getAdminMenuConfig();
        ConfigurationSection searchMenuCfg = config.getConfigurationSection("search-menu");

        this.title = getConfigString(searchMenuCfg, "title", "<dark_gray>Buscar Jugador");
        this.defaultText = getConfigString(searchMenuCfg, "default-text", "Escribe el nombre...");
        this.emptySearchMsg = getConfigString(searchMenuCfg, "empty-search-msg",
                "<red>Escribe un nombre de jugador valido.");
        this.playerNotFoundMsg = getConfigString(searchMenuCfg, "player-not-found-msg",
                "<red>No se encontro ningun jugador con ese nombre.");

        this.anvilInventory = Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.ANVIL,
                mm.deserialize(title));

        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = nameTag.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize("<gray><i>" + defaultText));
            nameTag.setItemMeta(meta);
        }
        anvilInventory.setItem(0, nameTag);

        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = barrier.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(mm.deserialize("<red><b>Clic para buscar</b>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Coloca el nombre del jugador"));
            lore.add(mm.deserialize("<gray>en la ranura izquierda"));
            bMeta.lore(lore);
            barrier.setItemMeta(bMeta);
        }
        anvilInventory.setItem(2, barrier);
    }

    public static void openSearchAnvil(AmplProtections plugin, Player player) {
        if (searchingPlayers.contains(player.getUniqueId())) {
            searchingPlayers.remove(player.getUniqueId());
        }
        SearchAnvilMenu menu = new SearchAnvilMenu(plugin, player);
        searchingPlayers.add(player.getUniqueId());
        player.openInventory(menu.anvilInventory);
        Bukkit.getPluginManager().registerEvents(menu, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker))
            return;
        if (!searchingPlayers.contains(clicker.getUniqueId()))
            return;

        Inventory top = event.getView().getTopInventory();
        if (!top.equals(anvilInventory))
            return;

        event.setCancelled(true);

        if (event.getRawSlot() == 2) {
            ItemStack input = top.getItem(0);
            if (input == null || input.getType() == Material.AIR) {
                clicker.sendMessage(mm.deserialize(emptySearchMsg));
                Location loc = clicker.getLocation();
                if (loc == null)
                    return;
                clicker.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                closeSearch(clicker);
                return;
            }

            ItemMeta inputMeta = input.getItemMeta();
            String rawName = inputMeta != null && inputMeta.hasDisplayName()
                    ? stripColor(legacy.serialize(inputMeta.displayName()))
                    : "";
            final String searchQuery = rawName;

            if (searchQuery.isEmpty() || searchQuery.equals(defaultText)) {
                clicker.sendMessage(mm.deserialize(emptySearchMsg));
                Location loc = clicker.getLocation();
                if (loc == null)
                    return;
                clicker.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                closeSearch(clicker);
                return;
            }

            List<org.bukkit.OfflinePlayer> matchedPlayers = new ArrayList<>();
            String lowerQuery = searchQuery.toLowerCase();
            for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                String name = op.getName();
                if (name != null && name.toLowerCase().contains(lowerQuery)) {
                    matchedPlayers.add(op);
                    if (matchedPlayers.size() >= 21)
                        break;
                }
            }

            closeSearch(clicker);

            if (matchedPlayers.isEmpty()) {
                clicker.sendMessage(mm.deserialize(playerNotFoundMsg.replace("%search%", searchQuery)));
                Location loc = clicker.getLocation();
                if (loc == null)
                    return;
                clicker.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }

            plugin.getMenuManager().openMenu(clicker, new SearchMenu(plugin, matchedPlayers, 1));
        }
    }

    @EventHandler
    public void onAnvilClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player closer))
            return;
        if (searchingPlayers.contains(closer.getUniqueId())) {
            closeSearch(closer);
        }
    }

    @EventHandler
    public void onAnvilDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player dragger))
            return;
        if (!searchingPlayers.contains(dragger.getUniqueId()))
            return;

        Inventory top = event.getView().getTopInventory();
        if (Objects.equals(top, anvilInventory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilSlotClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player cliccker))
            return;
        if (!searchingPlayers.contains(cliccker.getUniqueId()))
            return;

        Inventory top = event.getView().getTopInventory();
        if (!top.equals(anvilInventory))
            return;

        event.setCancelled(true);

        if (event.getRawSlot() == 0 && event.getClick().isLeftClick()) {
            ItemStack cursor = event.getCursor();
            if (cursor.getType() != Material.AIR) {
                ItemMeta cursorMeta = cursor.getItemMeta();
                if (cursorMeta != null && cursorMeta.hasDisplayName()) {
                    String cursorName = stripColor(legacy.serialize(cursorMeta.displayName())).trim();
                    if (!cursorName.isEmpty()) {
                        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
                        ItemMeta meta = nameTag.getItemMeta();
                        if (meta != null) {
                            meta.displayName(mm.deserialize("<gray><i>" + cursorName));
                            nameTag.setItemMeta(meta);
                        }
                        top.setItem(0, nameTag);
                        player.setItemOnCursor(new ItemStack(Material.AIR));
                        Location loc = player.getLocation();
                        if (loc == null)
                            return;
                        player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                    }
                }
            }
        }
    }

    private String stripColor(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("<[^>]*>", "").trim();
    }

    private void closeSearch(Player player) {
        searchingPlayers.remove(player.getUniqueId());
        player.closeInventory();
        org.bukkit.event.inventory.InventoryCloseEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryDragEvent.getHandlerList().unregister(this);
    }

    private String getConfigString(ConfigurationSection section, String path, String def) {
        return section != null ? section.getString(path, def) : def;
    }
}
