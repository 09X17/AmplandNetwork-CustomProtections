package com.amplan.amplprotections.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;

import com.amplan.amplprotections.AmplProtections;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLConnection {

    private final AmplProtections plugin;
    private HikariDataSource dataSource;
    private ExecutorService dbExecutor;

    public MySQLConnection(AmplProtections plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        FileConfiguration config = plugin.getConfig();

        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.name", "minecraft_server");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "password123");
        String properties = config.getString("database.properties",
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

        try {
            HikariConfig hikariConfig = new HikariConfig();

            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + properties);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);

            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setKeepaliveTime(0);
            hikariConfig.setConnectionTimeout(5000);

            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

            this.dataSource = new HikariDataSource(hikariConfig);

            AtomicInteger threadNum = new AtomicInteger(0);
            this.dbExecutor = Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "AmplProtections-DB-" + threadNum.getAndIncrement());
                t.setDaemon(true);
                return t;
            });

            try (Connection conn = dataSource.getConnection()) {
                plugin.getLogger().info("¡Conexión exitosa con el pool de MySQL de HikariCP!");
                createTables(conn);
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al conectar a la base de datos", e);
            return false;
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("El pool de conexiones no está listo o se encuentra cerrado.");
        }
        return dataSource.getConnection();
    }

    public ExecutorService getDbExecutor() {
        return dbExecutor;
    }

    public void disconnect() {
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Pool de conexiones de MySQL cerrado de forma segura.");
        }
    }

    private void createTables(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS ap_protections (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "land_id VARCHAR(32) DEFAULT NULL," +
                    "owner_uuid VARCHAR(36) NOT NULL," +
                    "world_name VARCHAR(64) NOT NULL," +
                    "custom_name VARCHAR(64) DEFAULT NULL," +
                    "custom_lore TEXT DEFAULT NULL," +
                    "block_x INT NOT NULL," +
                    "block_y INT NOT NULL," +
                    "block_z INT NOT NULL," +
                    "min_x INT NOT NULL," +
                    "max_x INT NOT NULL," +
                    "min_z INT NOT NULL," +
                    "max_z INT NOT NULL," +
                    "block_type VARCHAR(64) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX (owner_uuid)," +
                    "INDEX (world_name, min_x, max_x, min_z, max_z)," +
                    "UNIQUE INDEX (land_id)" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_protection_members (" +
                    "protection_id INT NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "rank_level VARCHAR(32) NOT NULL," +
                    "PRIMARY KEY (protection_id, player_uuid)," +
                    "FOREIGN KEY (protection_id) REFERENCES ap_protections(id) ON DELETE CASCADE" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_protection_flags (" +
                    "protection_id INT NOT NULL," +
                    "flag_key VARCHAR(64) NOT NULL," +
                    "flag_value TINYINT DEFAULT 0," +
                    "PRIMARY KEY (protection_id, flag_key)," +
                    "FOREIGN KEY (protection_id) REFERENCES ap_protections(id) ON DELETE CASCADE" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_protection_merges (" +
                    "merged_id INT NOT NULL," +
                    "original_id INT NOT NULL," +
                    "original_world VARCHAR(64) NOT NULL," +
                    "original_name VARCHAR(64) DEFAULT NULL," +
                    "original_lore TEXT DEFAULT NULL," +
                    "original_x INT NOT NULL," +
                    "original_y INT NOT NULL," +
                    "original_z INT NOT NULL," +
                    "original_min_x INT NOT NULL," +
                    "original_max_x INT NOT NULL," +
                    "original_min_z INT NOT NULL," +
                    "original_max_z INT NOT NULL," +
                    "original_type VARCHAR(64) NOT NULL," +
                    "original_land_id VARCHAR(32) DEFAULT NULL," +
                    "PRIMARY KEY (merged_id, original_id)" +
                    ");");

            migrateDatabase(connection);
        }
    }

    private void migrateDatabase(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM ap_protections LIKE 'custom_lore'")) {
                if (!rs.next()) {
                    stmt.execute(
                            "ALTER TABLE ap_protections ADD COLUMN custom_lore TEXT DEFAULT NULL AFTER custom_name");
                    plugin.getLogger().info("Migracion: columna custom_lore agregada a ap_protections");
                }
            }

            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM ap_protection_flags LIKE 'flag_key'")) {
                if (!rs.next()) {
                    stmt.execute("ALTER TABLE ap_protection_flags ADD COLUMN flag_key VARCHAR(64) NOT NULL DEFAULT ''");
                    stmt.execute(
                            "ALTER TABLE ap_protection_flags DROP PRIMARY KEY, ADD PRIMARY KEY (protection_id, flag_key)");
                    plugin.getLogger().info("Migracion: estructura de ap_protection_flags actualizada");
                }
            }

            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM ap_protections LIKE 'land_id'")) {
                if (!rs.next()) {
                    stmt.execute("ALTER TABLE ap_protections ADD COLUMN land_id VARCHAR(32) DEFAULT NULL AFTER id");
                    stmt.execute("ALTER TABLE ap_protections ADD UNIQUE INDEX (land_id)");
                    stmt.execute("UPDATE ap_protections SET land_id = CONCAT('LAND', id) WHERE land_id IS NULL");
                    plugin.getLogger().info("Migracion: columna land_id agregada y asignada a protecciones existentes");
                }
            }

            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM ap_protection_members LIKE 'joined_at'")) {
                if (!rs.next()) {
                    stmt.execute(
                            "ALTER TABLE ap_protection_members ADD COLUMN joined_at BIGINT DEFAULT 0 AFTER rank_level");
                    plugin.getLogger().info("Migracion: columna joined_at agregada a ap_protection_members");
                }
            }

            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM ap_protections LIKE 'hologram_enabled'")) {
                if (!rs.next()) {
                    stmt.execute("ALTER TABLE ap_protections ADD COLUMN hologram_enabled BOOLEAN DEFAULT FALSE");
                    plugin.getLogger().info("Migracion: columna hologram_enabled agregada a ap_protections");
                }
            }

            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM ap_protection_flags LIKE 'flag_value'")) {
                if (rs.next()) {
                    String columnType = rs.getString("Type");
                    if (columnType != null && columnType.toLowerCase().contains("tinyint(1)")) {
                        stmt.execute("ALTER TABLE ap_protection_flags MODIFY COLUMN flag_value TINYINT DEFAULT 0");
                        plugin.getLogger().info(
                                "Migracion: columna flag_value convertida de BOOLEAN a TINYINT para soportar niveles de permiso");
                    }
                }
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_rentals (" +
                    "region_id INT NOT NULL," +
                    "renter_uuid VARCHAR(36) NOT NULL," +
                    "start_time BIGINT NOT NULL," +
                    "end_time BIGINT NOT NULL," +
                    "price DECIMAL(10,2) DEFAULT 0.00," +
                    "auto_renew BOOLEAN DEFAULT FALSE," +
                    "last_payment BIGINT DEFAULT 0," +
                    "duration_days INT NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (region_id, renter_uuid)," +
                    "FOREIGN KEY (region_id) REFERENCES ap_protections(id) ON DELETE CASCADE," +
                    "INDEX (end_time)" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_block_log (" +
                    "log_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "region_id INT NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "old_type VARCHAR(64) DEFAULT NULL," +
                    "new_type VARCHAR(64) DEFAULT NULL," +
                    "block_data TEXT DEFAULT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "z INT NOT NULL," +
                    "timestamp BIGINT NOT NULL," +
                    "action VARCHAR(16) NOT NULL," +
                    "INDEX (region_id)," +
                    "INDEX (player_uuid)," +
                    "INDEX (timestamp)" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_flag_presets (" +
                    "preset_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(64) NOT NULL," +
                    "owner_uuid VARCHAR(36) NOT NULL," +
                    "is_global BOOLEAN DEFAULT FALSE," +
                    "flag_data TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX (owner_uuid)," +
                    "UNIQUE INDEX (name, owner_uuid)" +
                    ");");

            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM ap_rentals LIKE 'duration_days'")) {
                if (!rs.next()) {
                    stmt.execute(
                            "ALTER TABLE ap_rentals ADD COLUMN duration_days INT NOT NULL DEFAULT 0 AFTER last_payment");
                    plugin.getLogger().info("Migracion: columna duration_days agregada a ap_rentals");
                }
            }
        }
    }
}
