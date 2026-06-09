package com.amplan.amplprotections.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.database.ProtectionDao;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.model.ProtectionType;
import com.amplan.amplprotections.utils.MessageUtils;
import com.amplan.amplprotections.utils.SkullUtils;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ProtectionManager {

    private final AmplProtections plugin;
    private final ProtectionDao protectionDao;
    private final NamespacedKey protectionKey;

    private final Map<String, List<ProtectionRegion>> worldRegions = new ConcurrentHashMap<>();
    private final Map<Integer, ProtectionRegion> idMap = new HashMap<>();
    private final Map<String, ProtectionType> protectionTypes = new HashMap<>();
    private final Map<UUID, String> ownerNameCache = new ConcurrentHashMap<>();

    private final Set<String> validFlags;

    public ProtectionManager(AmplProtections plugin) {
        this.plugin = plugin;
        this.protectionDao = new ProtectionDao(plugin);
        this.protectionKey = new NamespacedKey(plugin, "protection-type");
        this.validFlags = new HashSet<>();
        loadFlags();
    }

    private void loadFlags() {
        validFlags.addAll(Objects.requireNonNullElse(
                plugin.getConfig().getStringList("flags.enabled"),
                new ArrayList<>()));
    }

    public void reloadFlags() {
        validFlags.clear();
        loadFlags();
    }

    public void saveRegionAsync(ProtectionRegion region) {
        protectionDao.saveRegionAsync(region);
    }

    public List<ProtectionRegion> getRegionsByOwner(UUID ownerUuid) {
        return idMap.values().stream()
                .filter(r -> r.getOwnerUniqueId().equals(ownerUuid))
                .collect(Collectors.toList());
    }

    public void loadProtectionTypes() {
        protectionTypes.clear();
        ConfigurationSection section = plugin.getBlocksConfig().getConfigurationSection("protection-blocks");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            try {
                ProtectionType type = new ProtectionType(
                        key,
                        Material.valueOf(section.getString(key + ".material")),
                        section.getInt(key + ".custom-model-data", -1),
                        section.getString(key + ".display-name", key),
                        section.getInt(key + ".radius", 5),
                        section.getString(key + ".skull-value", ""),
                        section.getBoolean(key + ".hide-block", false),
                        section.getBoolean(key + ".glow-on-view", true),
                        section.getBoolean(key + ".enable-merge", false),
                        section.getInt(key + ".max-merge-radius", 0));
                protectionTypes.put(key, type);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().log(Level.WARNING, "Error cargando tipo: {0}", key);
            }
        }
    }

    public void loadRegionsFromDatabase() {
        worldRegions.clear();
        idMap.clear();
        List<ProtectionRegion> loaded = protectionDao.loadAllRegionsSync();
        int maxId = 0;
        for (ProtectionRegion region : loaded) {
            worldRegions.computeIfAbsent(region.getWorldName(), k -> new CopyOnWriteArrayList<>()).add(region);
            idMap.put(region.getDatabaseId(), region);
            String landId = region.getLandId();
            if (landId != null && landId.startsWith("LAND")) {
                try {
                    int num = Integer.parseInt(landId.substring(4));
                    if (num > maxId)
                        maxId = num;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        ProtectionRegion.resetCounter(maxId);
    }

    public void clearAllRegions() {
        worldRegions.clear();
        idMap.clear();
        ProtectionRegion.resetCounter(0);
    }

    public ItemStack createProtectionItem(String typeId) {
        ProtectionType type = protectionTypes.get(typeId);
        if (type == null)
            return new ItemStack(Material.BEDROCK);
        ItemStack item;
        if (type.getMaterial() == Material.PLAYER_HEAD && type.getSkullValue() != null
                && !type.getSkullValue().isEmpty()) {
            item = SkullUtils.createSkullWithTexture(type.getSkullValue());
        } else {
            item = new ItemStack(type.getMaterial());
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(type.getDisplayName())
                    .decoration(TextDecoration.ITALIC, false));
            if (type.getCustomModelData() != -1)
                meta.setCustomModelData(type.getCustomModelData());
            meta.getPersistentDataContainer().set(protectionKey, PersistentDataType.STRING, typeId);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void createRegionFromBlock(Location loc, String typeId, Player owner) {
        ProtectionType type = protectionTypes.get(typeId);
        if (type == null)
            return;
        ProtectionRegion region = new ProtectionRegion(owner.getUniqueId(), loc, type);
        if (checkCollision(region)) {
            owner.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(MessageUtils.lang("protection-manager.collision-detected")));
            return;
        }
        worldRegions.computeIfAbsent(region.getWorldName(), k -> new CopyOnWriteArrayList<>()).add(region);
        protectionDao.saveRegionAsync(region).thenRun(() -> {
            idMap.put(region.getDatabaseId(), region);
        });
        owner.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(MessageUtils.lang("protection-manager.protection-established")));
    }

    public void deleteRegionAndDrop(ProtectionRegion region, Player player) {
        worldRegions.getOrDefault(region.getWorldName(), new CopyOnWriteArrayList<>()).remove(region);
        idMap.remove(region.getDatabaseId());
        plugin.getGlowManager().removeGlow(region);
        plugin.getHologramManager().removeHologram(region);
        protectionDao.deleteRegionAsync(region.getDatabaseId());
        player.getWorld().dropItemNaturally(
                new Location(player.getWorld(), region.getBlockX(), region.getBlockY(), region.getBlockZ()),
                createProtectionItem(region.getTypeId()));
    }

    public void removeRegion(ProtectionRegion region) {
        unmergeOnDelete(region);
        worldRegions.getOrDefault(region.getWorldName(), new CopyOnWriteArrayList<>()).remove(region);
        idMap.remove(region.getDatabaseId());
        plugin.getGlowManager().removeGlow(region);
        plugin.getHologramManager().removeHologram(region);
        protectionDao.deleteRegionAsync(region.getDatabaseId());
    }

    public void saveAllRegionsSync() {
        if (idMap.isEmpty())
            return;
        try (Connection conn = plugin.getDatabaseConnection().getConnection()) {
            conn.setAutoCommit(false);
            try {
                String updateSql = "UPDATE ap_protections SET custom_name = ?, custom_lore = ? WHERE id = ?";
                String deleteFlagsSql = "DELETE FROM ap_protection_flags WHERE protection_id = ?";
                String insertFlagSql = "INSERT INTO ap_protection_flags (protection_id, flag_key, flag_value) VALUES (?,?,?)";

                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                        PreparedStatement psDelete = conn.prepareStatement(deleteFlagsSql);
                        PreparedStatement psInsert = conn.prepareStatement(insertFlagSql)) {

                    for (ProtectionRegion region : idMap.values()) {
                        psUpdate.setString(1, region.getCustomName());
                        psUpdate.setString(2, region.getCustomLore());
                        psUpdate.setInt(3, region.getDatabaseId());
                        psUpdate.addBatch();

                        psDelete.setInt(1, region.getDatabaseId());
                        psDelete.addBatch();

                        for (Map.Entry<String, com.amplan.amplprotections.model.FlagPermissionLevel> entry : region
                                .getFlags().entrySet()) {
                            if (com.amplan.amplprotections.model.FlagPermissionLevel.isEnvironmental(entry.getKey())) continue;
                            psInsert.setInt(1, region.getDatabaseId());
                            psInsert.setString(2, entry.getKey());
                            psInsert.setInt(3, entry.getValue().getValue());
                            psInsert.addBatch();
                        }
                        for (Map.Entry<String, Boolean> entry : region.getBooleanFlags().entrySet()) {
                            psInsert.setInt(1, region.getDatabaseId());
                            psInsert.setString(2, entry.getKey());
                            psInsert.setInt(3, entry.getValue() ? 1 : 0);
                            psInsert.addBatch();
                        }
                    }

                    psUpdate.executeBatch();
                    psDelete.executeBatch();
                    psInsert.executeBatch();
                    conn.commit();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error during batch save, rolling back", e);
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to restore auto-commit", e);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error guardando regiones al desactivar", e);
        }
    }

    public String getCachedOwnerName(UUID ownerUuid) {
        return ownerNameCache.computeIfAbsent(ownerUuid, uuid -> {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
        });
    }

    public void clearOwnerNameCache() {
        ownerNameCache.clear();
    }

    public void createRegion(ProtectionRegion region) {
        worldRegions.computeIfAbsent(region.getWorldName(), k -> new CopyOnWriteArrayList<>()).add(region);
        protectionDao.saveRegionAsync(region).thenRun(() -> {
            idMap.put(region.getDatabaseId(), region);
        });
    }

    public List<ProtectionRegion> getAllRegions() {
        return new ArrayList<>(idMap.values());
    }

    public void addMember(ProtectionRegion region, UUID playerUuid, String rank) {
        region.addMember(playerUuid, com.amplan.amplprotections.model.PlayerRank.valueOf(rank));
        protectionDao.saveMemberAsync(region.getDatabaseId(), playerUuid, rank);
    }

    public void removeMember(ProtectionRegion region, UUID playerUuid) {
        region.removeMember(playerUuid);
        protectionDao.deleteMemberAsync(region.getDatabaseId(), playerUuid);
    }

    public ProtectionRegion getRegionAt(Location loc) {
        return worldRegions.getOrDefault(loc.getWorld().getName(), new ArrayList<>())
                .stream().filter(r -> r.contains(loc)).findFirst().orElse(null);
    }

    public boolean checkCollision(ProtectionRegion newRegion) {
        return worldRegions.getOrDefault(newRegion.getWorldName(), new ArrayList<>())
                .stream().anyMatch(r -> r.intersects(newRegion));
    }

    public ProtectionType getProtectionType(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        String typeId = item.getItemMeta().getPersistentDataContainer().get(protectionKey, PersistentDataType.STRING);
        return protectionTypes.get(typeId);
    }

    public ProtectionDao getProtectionDao() {
        return protectionDao;
    }

    public boolean isValidFlag(String flag) {
        return validFlags.contains(flag);
    }

    public Set<String> getAllAllowedFlags() {
        return validFlags;
    }

    public boolean isValidProtectionType(String typeId) {
        return protectionTypes.containsKey(typeId);
    }

    public Map<String, ProtectionType> getProtectionTypes() {
        return protectionTypes;
    }

    public void createRegionFromBuy(Player player, String typeId) {
        ProtectionType type = protectionTypes.get(typeId);
        if (type == null)
            return;

        if (!player.hasPermission("amplprotections.admin.bypass")) {
            if (plugin.getConfig().getStringList("worlds.disabled-worlds").contains(player.getWorld().getName())) {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        MessageUtils.lang("admin.world-disabled")));
                return;
            }

            String worldName = player.getWorld().getName();
            int maxPerWorld = plugin.getConfig().getInt("worlds.max-protections-per-world", 5);
            long countInWorld = worldRegions.getOrDefault(worldName, new ArrayList<>()).stream()
                    .filter(r -> r.getOwnerUniqueId().equals(player.getUniqueId())).count();
            if (countInWorld >= maxPerWorld) {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        MessageUtils.lang("buy.already-max")));
                return;
            }

            int typeLimit = getPlayerTypeLimit(player, typeId);
            long totalOwned = countPlayerTypeProtections(player, typeId);
            if (totalOwned >= typeLimit) {
                String typeName = type.getDisplayName();
                String limitMsg = MessageUtils.lang("admin.limit-reached-type")
                        .replace("%type%", typeName).replace("%limit%", String.valueOf(typeLimit));
                player.sendMessage(
                        net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(limitMsg));
                return;
            }
        }

        double cost = plugin.getEconomyManager().getCost(typeId);
        if (plugin.getEconomyManager().isEnabled() && !plugin.getEconomyManager().hasBypass(player)) {
            if (!plugin.getEconomyManager().hasBalance(player, cost)) {
                String msg = MessageUtils.lang("economy.insufficient-funds")
                        .replace("%amount%", plugin.getEconomyManager().format(cost));
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
                return;
            }
            if (!plugin.getEconomyManager().charge(player, cost)) {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        MessageUtils.lang("buy.purchase-failed")));
                return;
            }
        }

        ItemStack item = createProtectionItem(typeId);
        if (player.getInventory().firstEmpty() == -1) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.getWorld().dropItemNaturally(loc, item);
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    MessageUtils.lang("buy.item-dropped")));
        } else {
            player.getInventory().addItem(item);
        }

        String successMsg = MessageUtils.lang("buy.purchase-success")
                .replace("%type%", type.getDisplayName())
                .replace("%price%",
                        plugin.getEconomyManager().isEnabled() ? plugin.getEconomyManager().format(cost) : "0");
        player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(successMsg));

        executeBuyCommands(player, type, cost);
    }

    private void executeBuyCommands(Player player, ProtectionType type, double cost) {
        java.util.List<String> commands = plugin.getConfig().getStringList("buy-commands." + type.getId());
        if (commands.isEmpty())
            return;

        int radius = type.getRadius();
        String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);

        for (String cmd : commands) {
            String processed = cmd
                    .replace("%player%", player.getName())
                    .replace("%type%", type.getId())
                    .replace("%price%", String.valueOf(cost))
                    .replace("%size%", size)
                    .replace("%radius%", String.valueOf(radius));

            if (processed.toLowerCase().startsWith("player: ")) {
                String playerCmd = processed.substring(8).trim();
                player.performCommand(playerCmd);
            } else if (processed.toLowerCase().startsWith("console: ")) {
                String consoleCmd = processed.substring(9).trim();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCmd);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
            }
        }
    }

    private int getPlayerLimit(Player player) {
        ConfigurationSection limitsSection = plugin.getConfig().getConfigurationSection("limits");
        if (limitsSection == null)
            return 3;
        int limit = limitsSection.getInt("default", 3);
        for (String key : limitsSection.getKeys(false)) {
            if ("by-type".equals(key))
                continue;
            if (player.hasPermission("amplprotections.limit." + key)) {
                limit = Math.max(limit, limitsSection.getInt(key, limit));
            }
        }
        return limit;
    }

    public int getPlayerTypeLimit(Player player, String typeId) {
        ConfigurationSection limitsSection = plugin.getConfig().getConfigurationSection("limits");
        if (limitsSection == null)
            return Integer.MAX_VALUE;

        int globalLimit = getPlayerLimit(player);

        ConfigurationSection byTypeSection = limitsSection.getConfigurationSection("by-type");
        if (byTypeSection != null && byTypeSection.contains(typeId)) {
            ConfigurationSection typeSection = byTypeSection.getConfigurationSection(typeId);
            if (typeSection != null) {
                for (String rankKey : typeSection.getKeys(false)) {
                    if (player.hasPermission("amplprotections.limit." + rankKey)) {
                        int typeLimit = typeSection.getInt(rankKey);
                        if (typeLimit < globalLimit)
                            globalLimit = typeLimit;
                    }
                }
            }
        }

        return globalLimit;
    }

    public int countPlayerTypeProtections(Player player, String typeId) {
        return (int) getRegionsByOwner(player.getUniqueId()).stream()
                .filter(r -> r.getTypeId().equals(typeId))
                .count();
    }

    public List<ProtectionRegion> getAdjacentRegions(ProtectionRegion region) {
        int maxDist = plugin.getConfig().getInt("merge.max-distance", 5);
        List<ProtectionRegion> adjacent = new ArrayList<>();

        for (ProtectionRegion other : worldRegions.getOrDefault(region.getWorldName(), new ArrayList<>())) {
            if (other.getDatabaseId() == region.getDatabaseId())
                continue;
            if (!other.getOwnerUniqueId().equals(region.getOwnerUniqueId()))
                continue;

            int dx = Math.abs(region.getBlockX() - other.getBlockX());
            int dz = Math.abs(region.getBlockZ() - other.getBlockZ());
            int gapX = dx - region.getRadius() - other.getRadius();
            int gapZ = dz - region.getRadius() - other.getRadius();
            int gap = Math.max(gapX, gapZ);

            if (gap <= maxDist) {
                adjacent.add(other);
            }
        }

        return adjacent;
    }

    public void mergeRegions(List<ProtectionRegion> regions, Player player) {
        if (regions.size() < 2) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    MessageUtils.lang("merge.no-adjacent")));
            return;
        }

        UUID ownerUuid = regions.get(0).getOwnerUniqueId();
        for (ProtectionRegion r : regions) {
            if (!r.getOwnerUniqueId().equals(ownerUuid)) {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        MessageUtils.lang("merge.same-owner-only")));
                return;
            }
        }

        String sameTypeId = regions.get(0).getTypeId();
        for (ProtectionRegion r : regions) {
            if (!r.getTypeId().equals(sameTypeId)) {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        MessageUtils.lang("merge.different-type")));
                return;
            }
        }

        ProtectionType sourceType = protectionTypes.get(sameTypeId);
        if (sourceType == null || !sourceType.isEnableMerge()) {
            String typeName = sourceType != null ? sourceType.getDisplayName() : sameTypeId;
            String msg = MessageUtils.lang("merge.max-tier-reached").replace("%type%", typeName);
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
            return;
        }

        int mergedRadius = sourceType.getRadius() * regions.size();
        if (mergedRadius > sourceType.getMaxMergeRadius()) {
            String msg = MessageUtils.lang("merge.tier-limit-exceeded")
                    .replace("%count%", String.valueOf(regions.size()))
                    .replace("%radius%", String.valueOf(mergedRadius))
                    .replace("%max%", String.valueOf(sourceType.getMaxMergeRadius()))
                    .replace("%type%", sourceType.getDisplayName());
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
            return;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (ProtectionRegion r : regions) {
            minX = Math.min(minX, r.getMinX());
            maxX = Math.max(maxX, r.getMaxX());
            minZ = Math.min(minZ, r.getMinZ());
            maxZ = Math.max(maxZ, r.getMaxZ());
        }

        for (ProtectionRegion other : worldRegions.getOrDefault(regions.get(0).getWorldName(), new ArrayList<>())) {
            boolean isBeingMerged = false;
            for (ProtectionRegion r : regions) {
                if (r.getDatabaseId() == other.getDatabaseId()) {
                    isBeingMerged = true;
                    break;
                }
            }
            if (isBeingMerged)
                continue;

            boolean overlaps = (minX <= other.getMaxX() && maxX >= other.getMinX() &&
                    minZ <= other.getMaxZ() && maxZ >= other.getMinZ());
            if (overlaps) {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        MessageUtils.lang("merge.merge-collision")));
                return;
            }
        }

        double cost = plugin.getConfig().getDouble("merge.cost", 50.0);
        String bypassPerm = Objects.requireNonNullElse(
                plugin.getConfig().getString("merge.bypass-permission"),
                "amplprotections.economy.bypass");
        if (plugin.getEconomyManager().isEnabled() && !player.hasPermission(bypassPerm) && cost > 0) {
            if (!plugin.getEconomyManager().hasBalance(player, cost)) {
                String msg = MessageUtils.lang("economy.insufficient-funds")
                        .replace("%amount%", plugin.getEconomyManager().format(cost));
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
                return;
            }
            if (!plugin.getEconomyManager().charge(player, cost)) {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        MessageUtils.lang("merge.merge-failed")));
                return;
            }
        }

        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int centerY = regions.get(0).getBlockY();
        String worldName = regions.get(0).getWorldName();

        ProtectionType resultType = findTypeByRadius(mergedRadius);
        if (resultType == null)
            resultType = sourceType;

        Location centerLoc = findGroundBelow(new Location(Bukkit.getWorld(worldName), centerX, centerY, centerZ));

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < regions.size(); i++) {
            if (i > 0)
                nameBuilder.append(" + ");
            nameBuilder.append(regions.get(i).getCustomName());
        }
        String mergedName = nameBuilder.toString();
        if (mergedName.length() > 64)
            mergedName = mergedName.substring(0, 61) + "...";

        ProtectionRegion merged = new ProtectionRegion(
                -1, ownerUuid, worldName, mergedName,
                regions.get(0).getCustomLore(),
                centerLoc.getBlockX(), centerLoc.getBlockY(), centerLoc.getBlockZ(),
                minX, maxX, minZ, maxZ, -64, 320,
                resultType.getId(), null);

        for (ProtectionRegion r : regions) {
            for (Map.Entry<String, com.amplan.amplprotections.model.FlagPermissionLevel> entry : r.getFlags()
                    .entrySet()) {
                if (!merged.getFlags().containsKey(entry.getKey())) {
                    merged.setFlagLevel(entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry<String, Boolean> entry : r.getBooleanFlags().entrySet()) {
                if (!merged.getBooleanFlags().containsKey(entry.getKey())) {
                    merged.setBooleanFlag(entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry<java.util.UUID, com.amplan.amplprotections.model.PlayerRank> entry : r.getMembers()
                    .entrySet()) {
                if (!merged.getMembers().containsKey(entry.getKey())) {
                    merged.addMember(entry.getKey(), entry.getValue());
                }
            }
        }

        for (ProtectionRegion r : regions) {
            removeBlockFromWorld(r);
        }

        placeProtectionBlock(centerLoc, resultType);
        createRegion(merged);

        protectionDao.saveMergeHistorySync(merged.getDatabaseId(), regions);

        for (ProtectionRegion r : regions) {
            removeRegionWithoutUnmerge(r);
        }

        int width = maxX - minX + 1;
        int length = maxZ - minZ + 1;
        String size = width + "x" + length;
        String successMsg = MessageUtils.lang("merge.merge-success").replace("%size%", size);
        player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(successMsg));

        if (plugin.getEconomyManager().isEnabled() && cost > 0 && !player.hasPermission(bypassPerm)) {
            String costMsg = MessageUtils.lang("merge.merge-cost")
                    .replace("%amount%", plugin.getEconomyManager().format(cost));
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(costMsg));
        }
    }

    public MergeSimulation simulateMerge(List<ProtectionRegion> regions) {
        if (regions.size() < 2)
            return null;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (ProtectionRegion r : regions) {
            minX = Math.min(minX, r.getMinX());
            maxX = Math.max(maxX, r.getMaxX());
            minZ = Math.min(minZ, r.getMinZ());
            maxZ = Math.max(maxZ, r.getMaxZ());
        }
        int width = maxX - minX + 1;
        int length = maxZ - minZ + 1;
        int area = width * length;

        String sameTypeId = regions.get(0).getTypeId();
        for (ProtectionRegion r : regions) {
            if (!r.getTypeId().equals(sameTypeId)) {
                return new MergeSimulation(width, length, area, null, false, "Tipos diferentes");
            }
        }

        ProtectionType sourceType = protectionTypes.get(sameTypeId);
        if (sourceType == null) {
            return new MergeSimulation(width, length, area, null, false, "Tipo desconocido");
        }

        int mergedRadius = sourceType.getRadius() * regions.size();

        boolean canMerge = true;
        String rejectReason = null;

        if (!sourceType.isEnableMerge()) {
            canMerge = false;
            rejectReason = "Tipo no fusionable";
        } else if (mergedRadius > sourceType.getMaxMergeRadius()) {
            canMerge = false;
            rejectReason = "Excede límite del tier";
        }

        return new MergeSimulation(width, length, area, sourceType, canMerge, rejectReason);
    }

    public static class MergeSimulation {
        private final int width;
        private final int length;
        private final int area;
        private final ProtectionType largestType;
        private final boolean canMerge;
        private final String rejectReason;

        public MergeSimulation(int width, int length, int area, ProtectionType largestType, boolean canMerge,
                String rejectReason) {
            this.width = width;
            this.length = length;
            this.area = area;
            this.largestType = largestType;
            this.canMerge = canMerge;
            this.rejectReason = rejectReason;
        }

        public int getWidth() {
            return width;
        }

        public int getLength() {
            return length;
        }

        public int getArea() {
            return area;
        }

        public ProtectionType getLargestType() {
            return largestType;
        }

        public boolean canMerge() {
            return canMerge;
        }

        public String getRejectReason() {
            return rejectReason;
        }
    }

    private ProtectionType findTypeByRadius(int radius) {
        ProtectionType exact = null;
        for (ProtectionType type : protectionTypes.values()) {
            if (type.getRadius() == radius) {
                exact = type;
                break;
            }
        }
        if (exact != null)
            return exact;

        ProtectionType closest = null;
        for (ProtectionType type : protectionTypes.values()) {
            if (type.getRadius() >= radius) {
                if (closest == null || type.getRadius() < closest.getRadius()) {
                    closest = type;
                }
            }
        }
        return closest;
    }

    private Location findGroundBelow(Location loc) {
        Location current = loc.clone();
        org.bukkit.World world = current.getWorld();
        if (world == null)
            return loc;
        while (current.getBlockY() > world.getMinHeight()) {
            current.subtract(0, 1, 0);
            if (current.getBlock().getType().isSolid()) {
                current.add(0, 1, 0);
                return current;
            }
        }
        return loc;
    }

    private void removeBlockFromWorld(ProtectionRegion region) {
        org.bukkit.World world = Bukkit.getWorld(region.getWorldName());
        if (world == null)
            return;
        Location loc = new Location(world, region.getBlockX(), region.getBlockY(), region.getBlockZ());
        org.bukkit.block.Block block = loc.getBlock();
        ProtectionType type = protectionTypes.get(region.getTypeId());
        if (type != null && block.getType() == type.getMaterial()) {
            block.setType(org.bukkit.Material.AIR);
        }
    }

    private void placeProtectionBlock(Location loc, ProtectionType type) {
        org.bukkit.block.Block block = loc.getBlock();
        if (block.getType() == org.bukkit.Material.AIR) {
            block.setType(type.getMaterial());
        }
    }

    private void removeRegionWithoutUnmerge(ProtectionRegion region) {
        worldRegions.getOrDefault(region.getWorldName(), new CopyOnWriteArrayList<>()).remove(region);
        idMap.remove(region.getDatabaseId());
        plugin.getGlowManager().removeGlow(region);
        plugin.getHologramManager().removeHologram(region);
        protectionDao.deleteRegionAsync(region.getDatabaseId());
    }

    public void unmergeOnDelete(ProtectionRegion mergedRegion) {
        List<ProtectionRegion> parents = protectionDao.getMergeParentsSync(mergedRegion.getDatabaseId());
        if (parents.isEmpty())
            return;

        for (ProtectionRegion parent : parents) {
            ProtectionType parentType = protectionTypes.get(parent.getTypeId());
            Location blockLoc = findGroundBelow(new Location(
                    Bukkit.getWorld(parent.getWorldName()),
                    parent.getBlockX(), parent.getBlockY(), parent.getBlockZ()));

            ProtectionRegion restored = new ProtectionRegion(
                    -1,
                    mergedRegion.getOwnerUniqueId(),
                    parent.getWorldName(),
                    parent.getCustomName(),
                    parent.getCustomLore(),
                    blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ(),
                    parent.getMinX(), parent.getMaxX(), parent.getMinZ(), parent.getMaxZ(),
                    -64, 320,
                    parent.getTypeId(),
                    null);

            if (parentType != null) {
                placeProtectionBlock(blockLoc, parentType);
            }

            createRegion(restored);
        }

        protectionDao.deleteMergeHistorySync(mergedRegion.getDatabaseId());
    }

    public void updateOwnerInMemory(ProtectionRegion region, UUID newOwnerUuid) {
        region.setOwnerUniqueId(newOwnerUuid);
        clearOwnerNameCache();
    }
}