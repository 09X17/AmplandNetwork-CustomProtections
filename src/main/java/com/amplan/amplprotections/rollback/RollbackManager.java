package com.amplan.amplprotections.rollback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.MessageUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class RollbackManager {

    private final AmplProtections plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public RollbackManager(AmplProtections plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<List<RollbackEntry>> getChanges(ProtectionRegion region, UUID playerUuid,
            long sinceTimestamp) {
        CompletableFuture<List<RollbackEntry>> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            List<RollbackEntry> entries = new ArrayList<>();
            String sql = "SELECT * FROM ap_block_log WHERE region_id = ? AND timestamp >= ?";
            if (playerUuid != null) {
                sql += " AND player_uuid = ?";
            }
            sql += " ORDER BY timestamp DESC";

            try (Connection conn = plugin.getDatabaseConnection().getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, region.getDatabaseId());
                ps.setLong(2, sinceTimestamp);
                if (playerUuid != null) {
                    ps.setString(3, playerUuid.toString());
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new RollbackEntry(
                                rs.getInt("log_id"),
                                rs.getInt("region_id"),
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getString("old_type"),
                                rs.getString("new_type"),
                                rs.getString("block_data"),
                                rs.getInt("x"),
                                rs.getInt("y"),
                                rs.getInt("z"),
                                rs.getLong("timestamp"),
                                rs.getString("action")));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error obteniendo cambios para rollback", e);
            }

            future.complete(entries);
        });

        return future;
    }

    public void executeRollback(ProtectionRegion region, UUID playerUuid, long sinceTimestamp, Player executor) {
        if (plugin.getBlockLogger() != null) {
            plugin.getBlockLogger().flushAll();
        }

        getChanges(region, playerUuid, sinceTimestamp).thenAccept(entries -> {
            if (entries.isEmpty()) {
                executor.sendMessage(mm.deserialize(MessageUtils.lang("rollback.no-changes")));
                return;
            }

            World world = Bukkit.getWorld(region.getWorldName());
            if (world == null) {
                executor.sendMessage(mm.deserialize(MessageUtils.lang("rollback.world-not-found")));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                int reverted = 0;
                List<Integer> revertedLogIds = new ArrayList<>();

                for (RollbackEntry entry : entries) {
                    Block block = world.getBlockAt(entry.x, entry.y, entry.z);

                    Material oldMat;
                    try {
                        oldMat = Material.valueOf(entry.oldType);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING, "Material invalido en rollback: {0}", entry.oldType);
                        continue;
                    }

                    block.setType(oldMat, false);

                    if (entry.blockData != null && !entry.blockData.isEmpty()) {
                        try {
                            BlockData blockData = Bukkit.createBlockData(entry.blockData);
                            block.setBlockData(blockData, false);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().log(Level.FINE,
                                    "Block data invalido en rollback para {0} en {1},{2},{3}",
                                    new Object[] { entry.oldType, entry.x, entry.y, entry.z });
                        }
                    }

                    reverted++;
                    revertedLogIds.add(entry.logId);
                }

                String msg = MessageUtils.lang("rollback.success").replace("%count%", String.valueOf(reverted));
                executor.sendMessage(mm.deserialize(msg));

                deleteRolledBackEntriesById(revertedLogIds);
            });
        });
    }

    private void deleteRolledBackEntriesById(List<Integer> logIds) {
        if (logIds.isEmpty())
            return;
        CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM ap_block_log WHERE log_id = ?";
            try (Connection conn = plugin.getDatabaseConnection().getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int logId : logIds) {
                    ps.setInt(1, logId);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error eliminando entradas de rollback", e);
            }
        });
    }

    public CompletableFuture<Integer> getChangeCount(ProtectionRegion region, UUID playerUuid, long sinceTimestamp) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            String sql = "SELECT COUNT(*) FROM ap_block_log WHERE region_id = ? AND timestamp >= ?";
            if (playerUuid != null) {
                sql += " AND player_uuid = ?";
            }

            try (Connection conn = plugin.getDatabaseConnection().getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, region.getDatabaseId());
                ps.setLong(2, sinceTimestamp);
                if (playerUuid != null) {
                    ps.setString(3, playerUuid.toString());
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        future.complete(rs.getInt(1));
                    } else {
                        future.complete(0);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error contando cambios", e);
                future.complete(0);
            }
        });

        return future;
    }

    public static class RollbackEntry {
        public final int logId;
        public final int regionId;
        public final UUID playerUuid;
        public final String oldType;
        public final String newType;
        public final String blockData;
        public final int x, y, z;
        public final long timestamp;
        public final String action;

        public RollbackEntry(int logId, int regionId, UUID playerUuid, String oldType, String newType, String blockData,
                int x, int y, int z, long timestamp, String action) {
            this.logId = logId;
            this.regionId = regionId;
            this.playerUuid = playerUuid;
            this.oldType = oldType;
            this.newType = newType;
            this.blockData = blockData;
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
            this.action = action;
        }
    }
}
