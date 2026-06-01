package com.amplan.amplprotections.rollback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.ProtectionRegion;

public class BlockLogger {

    private final AmplProtections plugin;
    private final Queue<BlockChange> pendingChanges = new ConcurrentLinkedQueue<>();
    private BukkitTask flushTask;
    private boolean enabled;
    private int flushIntervalTicks;
    private int maxAgeDays;

    public BlockLogger(AmplProtections plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection rollbackSection = plugin.getConfig().getConfigurationSection("rollback");
        if (rollbackSection == null) {
            enabled = true;
            flushIntervalTicks = 600;
            maxAgeDays = 7;
            return;
        }

        enabled = rollbackSection.getBoolean("enabled", true);
        flushIntervalTicks = rollbackSection.getInt("flush-interval-ticks", 600);
        maxAgeDays = rollbackSection.getInt("max-age-days", 7);
    }

    public void reload() {
        loadConfig();
        if (enabled) {
            startFlushTask();
        } else {
            stopFlushTask();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void startFlushTask() {
        if (flushTask != null)
            flushTask.cancel();
        if (!enabled)
            return;

        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushChanges, flushIntervalTicks,
                flushIntervalTicks);
    }

    public void stopFlushTask() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
    }

    public void logBlockChange(Player player, Block block, Material oldType, Material newType, String action) {
        if (!enabled || player == null)
            return;

        ProtectionRegion region = plugin.getProtectionManager().getRegionAt(block.getLocation());
        if (region == null)
            return;

        String blockData = block.getBlockData().getAsString();

        pendingChanges.add(new BlockChange(
                region.getDatabaseId(),
                player.getUniqueId(),
                oldType.name(),
                newType.name(),
                blockData,
                block.getX(),
                block.getY(),
                block.getZ(),
                System.currentTimeMillis(),
                action));
    }

    public void logBlockBreak(Player player, Block block) {
        logBlockChange(player, block, block.getType(), Material.AIR, "break");
    }

    public void logBlockPlace(Player player, Block block, Material oldType) {
        logBlockChange(player, block, oldType, block.getType(), "place");
    }

    private void flushChanges() {
        if (pendingChanges.isEmpty())
            return;

        List<BlockChange> batch = new ArrayList<>();
        BlockChange change;
        while ((change = pendingChanges.poll()) != null && batch.size() < 500) {
            batch.add(change);
        }

        if (batch.isEmpty())
            return;

        String sql = "INSERT INTO ap_block_log (region_id, player_uuid, old_type, new_type, block_data, x, y, z, timestamp, action) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = plugin.getDatabaseConnection().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (BlockChange bc : batch) {
                ps.setInt(1, bc.regionId);
                ps.setString(2, bc.playerUuid.toString());
                ps.setString(3, bc.oldType);
                ps.setString(4, bc.newType);
                ps.setString(5, bc.blockData);
                ps.setInt(6, bc.x);
                ps.setInt(7, bc.y);
                ps.setInt(8, bc.z);
                ps.setLong(9, bc.timestamp);
                ps.setString(10, bc.action);
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando block log batch", e);
            pendingChanges.addAll(batch);
        }

        cleanupOldEntries();
    }

    private void cleanupOldEntries() {
        long cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24L * 60L * 60L * 1000L);
        String sql = "DELETE FROM ap_block_log WHERE timestamp < ?";

        try (Connection conn = plugin.getDatabaseConnection().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cutoffTime);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                plugin.getLogger().log(Level.INFO, "Block log cleanup: {0} entradas antiguas eliminadas.", deleted);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error limpiando entradas antiguas de block log", e);
        }
    }

    public void flushAll() {
        flushChanges();
    }

    public void shutdown() {
        stopFlushTask();
        flushAll();
        pendingChanges.clear();
    }

    public static class BlockChange {
        public final int regionId;
        public final UUID playerUuid;
        public final String oldType;
        public final String newType;
        public final String blockData;
        public final int x, y, z;
        public final long timestamp;
        public final String action;

        public BlockChange(int regionId, UUID playerUuid, String oldType, String newType, String blockData, int x,
                int y, int z, long timestamp, String action) {
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
