package com.amplan.amplprotections.listener;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.model.ProtectionType;
import com.amplan.amplprotections.utils.MessageUtils;
import com.amplan.amplprotections.utils.SkullUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class BlockListener implements Listener {

    private final AmplProtections plugin;
    private final ProtectionManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public BlockListener(AmplProtections plugin) {
        this.plugin = plugin;
        this.manager = plugin.getProtectionManager();
    }

    private String msg(String key) {
        return MessageUtils.lang(key);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand().clone();

        ProtectionType type = manager.getProtectionType(itemInHand);
        if (type == null) {
            ProtectionRegion region = manager.getRegionAt(event.getBlockPlaced().getLocation());
            if (region != null && !region.isMember(player.getUniqueId())
                    && !player.hasPermission("amplprotections.admin.bypass")) {
                if (!region.isFlagEnabled("block-place")) {
                    event.setCancelled(true);
                }
            }
            if (!event.isCancelled()) {
                logBlockChange(player, event.getBlockPlaced(), event.getBlockReplacedState().getType(),
                        event.getBlockPlaced().getType(), "place");
            }
            return;
        }

        Location blockLocation = event.getBlockPlaced().getLocation();
        String worldName = blockLocation.getWorld().getName();

        List<String> disabledWorlds = plugin.getConfig().getStringList("worlds.disabled-worlds");
        if (disabledWorlds.contains(worldName)) {
            player.sendMessage(mm.deserialize(msg("admin.world-disabled")));
            event.setCancelled(true);
            return;
        }

        int maxPerWorld = plugin.getConfig().getInt("worlds.max-protections-per-world", 5);
        long playerProtectionsInWorld = manager.getRegionsByOwner(player.getUniqueId()).stream()
                .filter(r -> r.getWorldName().equals(worldName))
                .count();

        if (playerProtectionsInWorld >= maxPerWorld) {
            player.sendMessage(mm.deserialize("<red>Has alcanzado el limite de protecciones en este mundo."));
            event.setCancelled(true);
            return;
        }

        String typeId = type.getId();
        if (!hasProtectionSlot(player, typeId)) {
            String typeName = type.getDisplayName();
            String limitMsg = msg("admin.limit-reached-type");
            limitMsg = limitMsg.replace("%type%", typeName).replace("%limit%",
                    String.valueOf(manager.getPlayerTypeLimit(player, typeId)));
            player.sendMessage(mm.deserialize(limitMsg));
            event.setCancelled(true);
            return;
        }

        ProtectionRegion newRegion = new ProtectionRegion(player.getUniqueId(), blockLocation, type);

        if (manager.checkCollision(newRegion)) {
            player.sendMessage(mm.deserialize(msg("admin.collision-error")));
            event.setCancelled(true);
            return;
        }

        manager.createRegion(newRegion);
        int size = type.getRadius() * 2 + 1;
        player.sendMessage(mm.deserialize(msg("protection.created").replace("%size%", String.valueOf(size))));

        if (type.isHideBlock()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                event.getBlockPlaced().setType(Material.AIR);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();

        ProtectionRegion region = manager.getRegionAt(blockLoc);
        if (region == null)
            return;

        if (region.getBlockX() == blockLoc.getBlockX() &&
                region.getBlockY() == blockLoc.getBlockY() &&
                region.getBlockZ() == blockLoc.getBlockZ()) {

            Player player = event.getPlayer();
            UUID playerUuid = player.getUniqueId();

            if (!region.getOwnerUniqueId().equals(playerUuid)
                    && !player.hasPermission("amplprotections.admin.bypass")) {
                player.sendMessage(mm.deserialize(msg("admin.not-owner")));
                event.setCancelled(true);
                return;
            }

            FileConfiguration blocksConfig = plugin.getBlocksConfig();
            String typeId = region.getTypeId();
            String matName = blocksConfig.getString("protection-blocks." + typeId + ".material", "STONE");
            Material material;
            try {
                material = Material.valueOf(matName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(String.format(
                        "Material invalido en blocks.yml para tipo '%s': %s. Usando STONE.",
                        typeId,
                        matName));
                material = Material.STONE;
            }
            int customModelData = blocksConfig.getInt(
                    "protection-blocks." + typeId + ".custom-model-data",
                    -1);

            String displayName = blocksConfig.getString(
                    "protection-blocks." + typeId + ".display-name",
                    typeId);

            String skullValue = blocksConfig.getString(
                    "protection-blocks." + typeId + ".skull-value",
                    "");

            event.setDropItems(false);
            ItemStack protectionItem = manager.createProtectionItem(typeId);
            if (protectionItem == null) {
                protectionItem = material == Material.PLAYER_HEAD
                        && skullValue != null
                        && !skullValue.isEmpty()
                                ? SkullUtils.createSkullWithTexture(skullValue)
                                : new ItemStack(material, 1);
                ItemMeta meta = protectionItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(
                            MiniMessage.miniMessage().deserialize(
                                    displayName != null
                                            ? displayName
                                            : "<white>Protection Block</white>"));
                    if (customModelData != -1) {
                        meta.setCustomModelData(customModelData);
                    }
                    NamespacedKey key = new NamespacedKey(plugin, "protection-type");
                    meta.getPersistentDataContainer().set(
                            key,
                            PersistentDataType.STRING,
                            typeId);

                    protectionItem.setItemMeta(meta);
                }
            }

            manager.removeRegion(region);
            World world = blockLoc.getWorld();
            if (world != null) {
                world.dropItemNaturally(blockLoc, protectionItem);
            }
            player.sendMessage(
                    mm.deserialize(msg("protection.removed")));
        } else {
            if (!region.isMember(event.getPlayer().getUniqueId())
                    && !event.getPlayer().hasPermission("amplprotections.admin.bypass")) {
                if (!region.isFlagEnabled("block-break")) {
                    event.setCancelled(true);
                }
            }
            if (!event.isCancelled()) {
                logBlockChange(event.getPlayer(), block, block.getType(), Material.AIR, "break");
            }
        }
    }

    private void logBlockChange(Player player, Block block, Material oldType, Material newType, String action) {
        if (plugin.getBlockLogger() != null) {
            ProtectionRegion region = manager.getRegionAt(block.getLocation());
            if (region != null) {
                plugin.getBlockLogger().logBlockChange(player, block, oldType, newType, action);
            }
        }
    }

    private boolean hasProtectionSlot(Player player, String typeId) {
        int currentProtections = manager.countPlayerTypeProtections(player, typeId);
        int maxAllowed = manager.getPlayerTypeLimit(player, typeId);
        return currentProtections < maxAllowed;
    }
}
