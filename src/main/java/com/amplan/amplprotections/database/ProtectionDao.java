package com.amplan.amplprotections.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.FlagPermissionLevel;
import com.amplan.amplprotections.model.PlayerRank;
import com.amplan.amplprotections.model.ProtectionRegion;

public class ProtectionDao {

    private final AmplProtections plugin;
    private final MySQLConnection mySQL;

    public ProtectionDao(AmplProtections plugin) {
        this.plugin = plugin;
        this.mySQL = plugin.getMySQLConnection();
    }

    public List<ProtectionRegion> loadAllRegionsSync() {
        List<ProtectionRegion> regions = new ArrayList<>();
        String selectRegions = "SELECT * FROM ap_protections";

        try (Connection conn = mySQL.getConnection();
                PreparedStatement ps = conn.prepareStatement(selectRegions);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                ProtectionRegion region = new ProtectionRegion(
                        id, UUID.fromString(rs.getString("owner_uuid")),
                        rs.getString("world_name"), rs.getString("custom_name"),
                        rs.getString("custom_lore"),
                        rs.getInt("block_x"), rs.getInt("block_y"), rs.getInt("block_z"),
                        rs.getInt("min_x"), rs.getInt("max_x"), rs.getInt("min_z"), rs.getInt("max_z"),
                        -64, 320, rs.getString("block_type"),
                        rs.getString("land_id"));

                try (PreparedStatement psFlags = conn.prepareStatement(
                        "SELECT flag_key, flag_value FROM ap_protection_flags WHERE protection_id = ?")) {
                    psFlags.setInt(1, id);
                    try (ResultSet rsFlags = psFlags.executeQuery()) {
                        while (rsFlags.next()) {
                            String rawValue = rsFlags.getString("flag_value");
                            FlagPermissionLevel level;
                            if ("true".equalsIgnoreCase(rawValue)) {
                                level = FlagPermissionLevel.EVERYONE;
                            } else if ("false".equalsIgnoreCase(rawValue)) {
                                level = FlagPermissionLevel.NONE;
                            } else {
                                try {
                                    level = FlagPermissionLevel.fromInt(Integer.parseInt(rawValue));
                                } catch (NumberFormatException e) {
                                    level = FlagPermissionLevel.fromString(rawValue);
                                }
                            }
                            region.setFlagLevel(rsFlags.getString("flag_key"), level);
                        }
                    }
                }

                try (PreparedStatement psMembers = conn.prepareStatement(
                        "SELECT player_uuid, rank_level, joined_at FROM ap_protection_members WHERE protection_id = ?")) {
                    psMembers.setInt(1, id);
                    try (ResultSet rsMembers = psMembers.executeQuery()) {
                        while (rsMembers.next()) {
                            UUID memberUuid = UUID.fromString(rsMembers.getString("player_uuid"));
                            PlayerRank rank = PlayerRank.valueOf(rsMembers.getString("rank_level"));
                            long joinedAt = rsMembers.getLong("joined_at");
                            region.addMember(memberUuid, rank);
                            if (joinedAt > 0) {
                                region.setMemberJoinDate(memberUuid, joinedAt);
                            }
                        }
                    }
                }
                regions.add(region);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando regiones", e);
        }
        return regions;
    }

    public CompletableFuture<Void> saveRegionAsync(ProtectionRegion region) {
        return CompletableFuture.runAsync(() -> {
            if (region.getDatabaseId() == -1) {
                insertNewRegion(region);
            } else {
                updateRegion(region);
            }
        }, mySQL.getDbExecutor());
    }

    private void insertNewRegion(ProtectionRegion region) {
        String sql = "INSERT INTO ap_protections (land_id, owner_uuid, world_name, custom_name, custom_lore, block_x, block_y, block_z, min_x, max_x, min_z, max_z, block_type) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = mySQL.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, region.getLandId());
            ps.setString(2, region.getOwnerUniqueId().toString());
            ps.setString(3, region.getWorldName());
            ps.setString(4, region.getCustomName());
            ps.setString(5, region.getCustomLore());
            ps.setInt(6, region.getBlockX());
            ps.setInt(7, region.getBlockY());
            ps.setInt(8, region.getBlockZ());
            ps.setInt(9, region.getMinX());
            ps.setInt(10, region.getMaxX());
            ps.setInt(11, region.getMinZ());
            ps.setInt(12, region.getMaxZ());
            ps.setString(13, region.getTypeId());
            ps.executeUpdate();

            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    int generatedId = gk.getInt(1);
                    region.setDatabaseId(generatedId);
                }
            }
            saveFlags(region);
            saveMemberSync(region.getDatabaseId(), region.getOwnerUniqueId(), "OWNER");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error insertando región", e);
        }
    }

    private void saveMemberSync(int regionId, UUID playerUuid, String rank) {
        String sql = "INSERT INTO ap_protection_members (protection_id, player_uuid, rank_level, joined_at) VALUES (?, ?, ?, UNIX_TIMESTAMP() * 1000) "
                +
                "ON DUPLICATE KEY UPDATE rank_level = VALUES(rank_level)";
        try (Connection conn = mySQL.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, regionId);
            ps.setString(2, playerUuid.toString());
            ps.setString(3, rank);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando miembro", e);
        }
    }

    private void saveFlags(ProtectionRegion region) {
        String delete = "DELETE FROM ap_protection_flags WHERE protection_id = ?";
        String insert = "INSERT INTO ap_protection_flags (protection_id, flag_key, flag_value) VALUES (?,?,?)";
        try (Connection conn = mySQL.getConnection()) {
            try (PreparedStatement psD = conn.prepareStatement(delete)) {
                psD.setInt(1, region.getDatabaseId());
                psD.executeUpdate();
            }
            try (PreparedStatement psI = conn.prepareStatement(insert)) {
                for (Map.Entry<String, FlagPermissionLevel> entry : region.getFlags().entrySet()) {
                    if (FlagPermissionLevel.isEnvironmental(entry.getKey())) continue;
                    psI.setInt(1, region.getDatabaseId());
                    psI.setString(2, entry.getKey());
                    psI.setInt(3, entry.getValue().getValue());
                    psI.addBatch();
                }
                for (Map.Entry<String, Boolean> entry : region.getBooleanFlags().entrySet()) {
                    psI.setInt(1, region.getDatabaseId());
                    psI.setString(2, entry.getKey());
                    psI.setInt(3, entry.getValue() ? 1 : 0);
                    psI.addBatch();
                }
                psI.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando flags", e);
        }
    }

    private void updateRegion(ProtectionRegion region) {
        String sql = "UPDATE ap_protections SET custom_name = ?, custom_lore = ? WHERE id = ?";
        try (Connection conn = mySQL.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, region.getCustomName());
            ps.setString(2, region.getCustomLore());
            ps.setInt(3, region.getDatabaseId());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error actualizando región", e);
        }
        saveFlags(region);
    }

    public CompletableFuture<Void> updateLoreAsync(int regionId, String lore) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE ap_protections SET custom_lore = ? WHERE id = ?";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, lore);
                ps.setInt(2, regionId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error actualizando lore", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<Void> deleteRegionAsync(int regionId) {
        return CompletableFuture.runAsync(() -> {
            String deleteFlagsSQL = "DELETE FROM ap_protection_flags WHERE protection_id = ?";
            String deleteMembersSQL = "DELETE FROM ap_protection_members WHERE protection_id = ?";
            String deleteRegionSQL = "DELETE FROM ap_protections WHERE id = ?";
            try (Connection conn = mySQL.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement psFlags = conn.prepareStatement(deleteFlagsSQL);
                        PreparedStatement psMembers = conn.prepareStatement(deleteMembersSQL);
                        PreparedStatement psRegion = conn.prepareStatement(deleteRegionSQL)) {
                    psFlags.setInt(1, regionId);
                    psFlags.executeUpdate();
                    psMembers.setInt(1, regionId);
                    psMembers.executeUpdate();
                    psRegion.setInt(1, regionId);
                    psRegion.executeUpdate();
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error borrando región", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<Void> saveFlagAsync(int regionId, String flagKey, FlagPermissionLevel level) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO ap_protection_flags (protection_id, flag_key, flag_value) VALUES (?, ?, ?)";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, regionId);
                ps.setString(2, flagKey);
                ps.setInt(3, level.getValue());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error guardando flag", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<Void> saveBooleanFlagAsync(int regionId, String flagKey, boolean value) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO ap_protection_flags (protection_id, flag_key, flag_value) VALUES (?, ?, ?)";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, regionId);
                ps.setString(2, flagKey);
                ps.setInt(3, value ? 1 : 0);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error guardando boolean flag", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<Void> saveMemberAsync(int regionId, UUID playerUuid, String rank) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO ap_protection_members (protection_id, player_uuid, rank_level, joined_at) VALUES (?, ?, ?, UNIX_TIMESTAMP() * 1000) "
                    +
                    "ON DUPLICATE KEY UPDATE rank_level = VALUES(rank_level)";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, regionId);
                ps.setString(2, playerUuid.toString());
                ps.setString(3, rank);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error guardando miembro", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<Void> deleteMemberAsync(int regionId, UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM ap_protection_members WHERE protection_id = ? AND player_uuid = ?";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, regionId);
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error borrando miembro", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<Void> removeMemberAsync(int regionId, UUID playerUuid) {
        return deleteMemberAsync(regionId, playerUuid);
    }

    public CompletableFuture<Void> saveHologramStateAsync(int regionId, boolean enabled) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE ap_protections SET hologram_enabled = ? WHERE id = ?";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, enabled);
                ps.setInt(2, regionId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error guardando estado de holograma", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<Integer> savePresetAsync(com.amplan.amplprotections.model.FlagPreset preset) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO ap_flag_presets (name, owner_uuid, is_global, flag_data) VALUES (?, ?, ?, ?)";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, preset.getName());
                ps.setString(2, preset.getOwnerUuid().toString());
                ps.setBoolean(3, preset.isGlobal());

                com.google.gson.Gson gson = new com.google.gson.Gson();
                String flagData = gson.toJson(preset.getFlags());
                ps.setString(4, flagData);

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        future.complete(rs.getInt(1));
                    } else {
                        future.complete(-1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error guardando preset", e);
                future.complete(-1);
            }
        }, mySQL.getDbExecutor());
        return future;
    }

    public CompletableFuture<Void> deletePresetAsync(int presetId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM ap_flag_presets WHERE preset_id = ?";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, presetId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error borrando preset", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<List<com.amplan.amplprotections.model.FlagPreset>> loadAllPresetsAsync() {
        CompletableFuture<List<com.amplan.amplprotections.model.FlagPreset>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            List<com.amplan.amplprotections.model.FlagPreset> presets = new ArrayList<>();
            String sql = "SELECT * FROM ap_flag_presets";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {

                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.google.gson.reflect.TypeToken<java.util.Map<String, com.amplan.amplprotections.model.FlagPermissionLevel>> typeToken = new com.google.gson.reflect.TypeToken<java.util.Map<String, com.amplan.amplprotections.model.FlagPermissionLevel>>() {
                };

                while (rs.next()) {
                    int id = rs.getInt("preset_id");
                    String name = rs.getString("name");
                    UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                    boolean isGlobal = rs.getBoolean("is_global");
                    String description = rs.getString("name");
                    String flagData = rs.getString("flag_data");

                    com.amplan.amplprotections.model.FlagPreset preset = new com.amplan.amplprotections.model.FlagPreset(
                            id, name, ownerUuid, isGlobal, description);

                    try {
                        java.util.Map<String, String> flagMap = gson.fromJson(flagData, typeToken.getType());
                        for (java.util.Map.Entry<String, String> entry : flagMap.entrySet()) {
                            try {
                                com.amplan.amplprotections.model.FlagPermissionLevel level = com.amplan.amplprotections.model.FlagPermissionLevel
                                        .valueOf(entry.getValue().toUpperCase());
                                preset.setFlag(entry.getKey(), level);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    } catch (com.google.gson.JsonParseException ignored) {
                        plugin.getLogger().log(Level.WARNING, "Error cargando flags del preset: {0}",
                                Objects.requireNonNullElse(name, "desconocido"));
                    }

                    presets.add(preset);
                }
                future.complete(presets);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error cargando presets", e);
                future.complete(new ArrayList<>());
            }
        }, mySQL.getDbExecutor());
        return future;
    }

    public CompletableFuture<Void> saveFlagsAsync(ProtectionRegion region) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = mySQL.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    String deleteSql = "DELETE FROM ap_protection_flags WHERE protection_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                        ps.setInt(1, region.getDatabaseId());
                        ps.executeUpdate();
                    }

                    String insertSql = "INSERT INTO ap_protection_flags (protection_id, flag_key, flag_value) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        for (java.util.Map.Entry<String, FlagPermissionLevel> entry : region.getFlags().entrySet()) {
                            if (FlagPermissionLevel.isEnvironmental(entry.getKey())) continue;
                            ps.setInt(1, region.getDatabaseId());
                            ps.setString(2, entry.getKey());
                            ps.setInt(3, entry.getValue().getValue());
                            ps.addBatch();
                        }
                        for (java.util.Map.Entry<String, Boolean> entry : region.getBooleanFlags().entrySet()) {
                            ps.setInt(1, region.getDatabaseId());
                            ps.setString(2, entry.getKey());
                            ps.setInt(3, entry.getValue() ? 1 : 0);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error guardando flags", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<Void> saveRentalAsync(com.amplan.amplprotections.model.Rental rental) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO ap_rentals (region_id, renter_uuid, start_time, end_time, price, auto_renew, last_payment, duration_days) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE renter_uuid = VALUES(renter_uuid), start_time = VALUES(start_time), " +
                    "end_time = VALUES(end_time), price = VALUES(price), auto_renew = VALUES(auto_renew), last_payment = VALUES(last_payment), duration_days = VALUES(duration_days)";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, rental.getRegionId());
                ps.setString(2, rental.getRenterUuid().toString());
                ps.setLong(3, rental.getStartTime());
                ps.setLong(4, rental.getEndTime());
                ps.setDouble(5, rental.getPrice());
                ps.setBoolean(6, rental.isAutoRenew());
                ps.setLong(7, rental.getLastPayment());
                ps.setInt(8, rental.getDurationDays());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error guardando rental", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<Void> deleteRentalAsync(int regionId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM ap_rentals WHERE region_id = ?";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, regionId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error borrando rental", e);
            }
        }, mySQL.getDbExecutor());
    }

    public CompletableFuture<List<com.amplan.amplprotections.model.Rental>> loadAllRentalsAsync() {
        CompletableFuture<List<com.amplan.amplprotections.model.Rental>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            List<com.amplan.amplprotections.model.Rental> rentals = new ArrayList<>();
            String sql = "SELECT * FROM ap_rentals";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rentals.add(new com.amplan.amplprotections.model.Rental(
                            rs.getInt("region_id"),
                            UUID.fromString(rs.getString("renter_uuid")),
                            rs.getLong("start_time"),
                            rs.getLong("end_time"),
                            rs.getDouble("price"),
                            rs.getBoolean("auto_renew"),
                            rs.getLong("last_payment"),
                            rs.getInt("duration_days")));
                }
                future.complete(rentals);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error cargando rentals", e);
                future.complete(new ArrayList<>());
            }
        }, mySQL.getDbExecutor());
        return future;
    }

    public CompletableFuture<List<UUID>> getPlayersWithChangesAsync(int regionId) {
        CompletableFuture<List<UUID>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            List<UUID> players = new ArrayList<>();
            String sql = "SELECT player_uuid FROM ap_block_log WHERE region_id = ? GROUP BY player_uuid ORDER BY MAX(timestamp) DESC";
            try (Connection conn = mySQL.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, regionId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            players.add(UUID.fromString(rs.getString("player_uuid")));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                future.complete(players);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error obteniendo jugadores con cambios", e);
                future.complete(new ArrayList<>());
            }
        }, mySQL.getDbExecutor());
        return future;
    }

    public void saveMergeHistorySync(int mergedId, List<ProtectionRegion> originals) {
        String sql = "INSERT INTO ap_protection_merges (merged_id, original_id, original_world, original_name, original_lore, "
                +
                "original_x, original_y, original_z, original_min_x, original_max_x, original_min_z, original_max_z, " +
                "original_type, original_land_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = mySQL.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ProtectionRegion orig : originals) {
                ps.setInt(1, mergedId);
                ps.setInt(2, orig.getDatabaseId());
                ps.setString(3, orig.getWorldName());
                ps.setString(4, orig.getCustomName());
                ps.setString(5, orig.getCustomLore());
                ps.setInt(6, orig.getBlockX());
                ps.setInt(7, orig.getBlockY());
                ps.setInt(8, orig.getBlockZ());
                ps.setInt(9, orig.getMinX());
                ps.setInt(10, orig.getMaxX());
                ps.setInt(11, orig.getMinZ());
                ps.setInt(12, orig.getMaxZ());
                ps.setString(13, orig.getTypeId());
                ps.setString(14, orig.getLandId());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando historial de fusion", e);
        }
    }

    public List<ProtectionRegion> getMergeParentsSync(int mergedId) {
        List<ProtectionRegion> parents = new ArrayList<>();
        String sql = "SELECT * FROM ap_protection_merges WHERE merged_id = ?";
        try (Connection conn = mySQL.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mergedId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProtectionRegion parent = new ProtectionRegion(
                            rs.getInt("original_id"),
                            null,
                            rs.getString("original_world"),
                            rs.getString("original_name"),
                            rs.getString("original_lore"),
                            rs.getInt("original_x"), rs.getInt("original_y"), rs.getInt("original_z"),
                            rs.getInt("original_min_x"), rs.getInt("original_max_x"),
                            rs.getInt("original_min_z"), rs.getInt("original_max_z"),
                            -64, 320,
                            rs.getString("original_type"),
                            rs.getString("original_land_id"));
                    parents.add(parent);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando historial de fusion", e);
        }
        return parents;
    }

    public void deleteMergeHistorySync(int mergedId) {
        String sql = "DELETE FROM ap_protection_merges WHERE merged_id = ?";
        try (Connection conn = mySQL.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mergedId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error borrando historial de fusion", e);
        }
    }

    public boolean hasMergeHistory(int mergedId) {
        String sql = "SELECT COUNT(*) FROM ap_protection_merges WHERE merged_id = ?";
        try (Connection conn = mySQL.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mergedId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error verificando historial de fusion", e);
        }
        return false;
    }
}
