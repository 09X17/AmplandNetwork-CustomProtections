package com.amplan.amplprotections.hook;

import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.model.ProtectionRegion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final AmplProtections plugin;
    private final ProtectionManager manager;

    public PlaceholderAPIHook(AmplProtections plugin) {
        this.plugin = plugin;
        this.manager = plugin.getProtectionManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "AmplProtections";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AmplandDev";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {

        if (offlinePlayer == null || manager == null) {
            return "0";
        }

        Player player = offlinePlayer.getPlayer();
        if (params.equalsIgnoreCase("total")) {
            return String.valueOf(
                    manager.getRegionsByOwner(offlinePlayer.getUniqueId()).size());
        }

        if (params.equalsIgnoreCase("limit")) {
            if (player == null) {
                return String.valueOf(
                        plugin.getConfig().getInt("limits.default", 3));
            }
            return String.valueOf(
                    getMaxAllowedProtections(player));
        }

        if (params.startsWith("limit_")) {
            String typeId = params.substring(6);
            if (player == null) {
                ConfigurationSection defaults = plugin.getConfig()
                        .getConfigurationSection("limits.default");
                if (defaults != null && defaults.contains(typeId)) {
                    return String.valueOf(defaults.getInt(typeId));
                }

                return "0";
            }
            return String.valueOf(
                    manager.getPlayerTypeLimit(player, typeId));
        }
        if (params.startsWith("type_count_")) {
            String typeId = params.substring(11);
            return String.valueOf(
                    manager.getRegionsByOwner(offlinePlayer.getUniqueId())
                            .stream()
                            .filter(r -> r.getTypeId().equals(typeId))
                            .count());
        }

        if (params.equalsIgnoreCase("land_ids")) {
            List<ProtectionRegion> regions = manager.getRegionsByOwner(offlinePlayer.getUniqueId());

            if (regions.isEmpty()) {
                return "Ninguna";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < regions.size(); i++) {

                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(regions.get(i).getLandId());
            }
            return sb.toString();
        }

        if (params.equalsIgnoreCase("land_count")) {
            return String.valueOf(
                    manager.getRegionsByOwner(offlinePlayer.getUniqueId()).size());
        }

        if (player != null) {
            ProtectionRegion currentRegion = manager.getRegionAt(player.getLocation());
            if (currentRegion != null) {
                if (params.equalsIgnoreCase("current_land")) {
                    return currentRegion.getLandId();
                }
                if (params.equalsIgnoreCase("current_owner")) {
                    return currentRegion.getOwnerName();
                }
                if (params.equalsIgnoreCase("current_type")) {
                    return currentRegion.getTypeId()
                            .replace("_", " ")
                            .toUpperCase();
                }
                if (params.equalsIgnoreCase("current_size")) {

                    int radius = currentRegion.getRadius();

                    return (radius * 2 + 1)
                            + "x"
                            + (radius * 2 + 1);
                }

                if (params.equalsIgnoreCase("current_world")) {
                    return currentRegion.getWorldName();
                }

                if (params.equalsIgnoreCase("current_x")) {
                    return String.valueOf(currentRegion.getBlockX());
                }

                if (params.equalsIgnoreCase("current_y")) {
                    return String.valueOf(currentRegion.getBlockY());
                }

                if (params.equalsIgnoreCase("current_z")) {
                    return String.valueOf(currentRegion.getBlockZ());
                }

                if (params.equalsIgnoreCase("current_members")) {
                    return String.valueOf(
                            currentRegion.getMembers().size());
                }

                if (params.equalsIgnoreCase("current_rank")) {

                    var rank = currentRegion.getRank(player.getUniqueId());

                    return rank != null
                            ? rank.getDisplayName()
                            : "Forastero";
                }
            }
        }

        return null;
    }

    private int getMaxAllowedProtections(Player player) {
        if (player.hasPermission("amplprotections.limit.admin"))
            return 999;

        int totalOwned = manager.getRegionsByOwner(player.getUniqueId()).size();
        int minRemaining = Integer.MAX_VALUE;

        for (String typeId : manager.getProtectionTypes().keySet()) {
            int limit = manager.getPlayerTypeLimit(player, typeId);
            int owned = manager.countPlayerTypeProtections(player, typeId);
            int remaining = limit - owned;
            if (remaining < minRemaining) {
                minRemaining = remaining;
            }
        }

        if (minRemaining == Integer.MAX_VALUE)
            return 999;
        return totalOwned + Math.max(0, minRemaining);
    }
}
