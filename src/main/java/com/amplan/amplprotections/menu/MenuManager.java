package com.amplan.amplprotections.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

import com.amplan.amplprotections.AmplProtections;

public class MenuManager implements Listener {

    @SuppressWarnings("unused")
    private final AmplProtections plugin;
    private final Map<UUID, CustomMenu> activeMenus = new HashMap<>();

    public MenuManager(AmplProtections plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, CustomMenu menu) {
        activeMenus.put(player.getUniqueId(), menu);
        player.openInventory(menu.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        CustomMenu currentMenu = activeMenus.get(player.getUniqueId());
        if (currentMenu == null)
            return;

        event.setCancelled(true);

        if (event.getClickedInventory() != null &&
                Objects.equals(event.getClickedInventory(), event.getView().getTopInventory())) {
            currentMenu.handleMenuClick(event.getSlot(), player, event.getClick());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        CustomMenu closedMenu = activeMenus.get(player.getUniqueId());
        if (closedMenu != null && closedMenu.getInventory().equals(event.getInventory())) {
            activeMenus.remove(player.getUniqueId());
        }
    }

    public interface CustomMenu extends InventoryHolder {
        void handleMenuClick(int slot, Player player);

        default void handleMenuClick(int slot, Player player, org.bukkit.event.inventory.ClickType clickType) {
            handleMenuClick(slot, player);
        }
    }
}
