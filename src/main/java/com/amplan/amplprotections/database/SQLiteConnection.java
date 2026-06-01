package com.amplan.amplprotections.database;

import java.sql.Connection;
import java.sql.ResultSet;
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

public class SQLiteConnection implements DatabaseConnection {

    private final AmplProtections plugin;
    private HikariDataSource dataSource;
    private ExecutorService dbExecutor;
    private final SqlDialect dialect = new SQLiteDialect();

    public SQLiteConnection(AmplProtections plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean connect() {
        FileConfiguration config = plugin.getConfig();
        String dbFile = config.getString("database.sqlite.file", "data.db");

        String jdbcUrl = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/" + dbFile;

        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setKeepaliveTime(0);
            hikariConfig.setConnectionTimeout(5000);

            hikariConfig.addDataSourceProperty("journal_mode", "WAL");
            hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
            hikariConfig.addDataSourceProperty("foreign_keys", "ON");
            hikariConfig.addDataSourceProperty("busy_timeout", "5000");

            this.dataSource = new HikariDataSource(hikariConfig);

            AtomicInteger threadNum = new AtomicInteger(0);
            this.dbExecutor = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "AmplProtections-DB-" + threadNum.getAndIncrement());
                t.setDaemon(true);
                return t;
            });

            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute("PRAGMA foreign_keys = ON");
                plugin.getLogger().info("Conexion exitosa con SQLite: " + dbFile);
                createTables(conn);
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al conectar a SQLite", e);
            return false;
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("El pool de conexiones no esta listo o se encuentra cerrado.");
        }
        Connection conn = dataSource.getConnection();
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }

    @Override
    public ExecutorService getDbExecutor() {
        return dbExecutor;
    }

    @Override
    public void disconnect() {
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Pool de conexiones de SQLite cerrado de forma segura.");
        }
    }

    @Override
    public void createTables(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS ap_protections (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "land_id TEXT DEFAULT NULL," +
                    "owner_uuid TEXT NOT NULL," +
                    "world_name TEXT NOT NULL," +
                    "custom_name TEXT DEFAULT NULL," +
                    "custom_lore TEXT DEFAULT NULL," +
                    "block_x INT NOT NULL," +
                    "block_y INT NOT NULL," +
                    "block_z INT NOT NULL," +
                    "min_x INT NOT NULL," +
                    "max_x INT NOT NULL," +
                    "min_z INT NOT NULL," +
                    "max_z INT NOT NULL," +
                    "block_type TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "hologram_enabled INTEGER DEFAULT 0" +
                    ");");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_protections_owner ON ap_protections(owner_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_protections_location ON ap_protections(world_name, min_x, max_x, min_z, max_z)");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_protections_land_id ON ap_protections(land_id)");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_protection_members (" +
                    "protection_id INT NOT NULL," +
                    "player_uuid TEXT NOT NULL," +
                    "rank_level TEXT NOT NULL," +
                    "joined_at INTEGER DEFAULT 0," +
                    "PRIMARY KEY (protection_id, player_uuid)," +
                    "FOREIGN KEY (protection_id) REFERENCES ap_protections(id) ON DELETE CASCADE" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_protection_flags (" +
                    "protection_id INT NOT NULL," +
                    "flag_key TEXT NOT NULL," +
                    "flag_value INTEGER DEFAULT 0," +
                    "PRIMARY KEY (protection_id, flag_key)," +
                    "FOREIGN KEY (protection_id) REFERENCES ap_protections(id) ON DELETE CASCADE" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_protection_merges (" +
                    "merged_id INT NOT NULL," +
                    "original_id INT NOT NULL," +
                    "original_world TEXT NOT NULL," +
                    "original_name TEXT DEFAULT NULL," +
                    "original_lore TEXT DEFAULT NULL," +
                    "original_x INT NOT NULL," +
                    "original_y INT NOT NULL," +
                    "original_z INT NOT NULL," +
                    "original_min_x INT NOT NULL," +
                    "original_max_x INT NOT NULL," +
                    "original_min_z INT NOT NULL," +
                    "original_max_z INT NOT NULL," +
                    "original_type TEXT NOT NULL," +
                    "original_land_id TEXT DEFAULT NULL," +
                    "PRIMARY KEY (merged_id, original_id)" +
                    ");");

            migrateDatabase(connection);
        }
    }

    private void migrateDatabase(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            if (!columnExists(connection, "ap_protections", "custom_lore")) {
                stmt.execute("ALTER TABLE ap_protections ADD COLUMN custom_lore TEXT DEFAULT NULL");
                plugin.getLogger().info("Migracion: columna custom_lore agregada a ap_protections");
            }

            if (!columnExists(connection, "ap_protection_flags", "flag_key")) {
                stmt.execute("ALTER TABLE ap_protection_flags ADD COLUMN flag_key TEXT NOT NULL DEFAULT ''");
                plugin.getLogger().info("Migracion: estructura de ap_protection_flags actualizada");
            }

            if (!columnExists(connection, "ap_protections", "land_id")) {
                stmt.execute("ALTER TABLE ap_protections ADD COLUMN land_id TEXT DEFAULT NULL");
                stmt.execute("UPDATE ap_protections SET land_id = 'LAND' || id WHERE land_id IS NULL");
                plugin.getLogger().info("Migracion: columna land_id agregada y asignada a protecciones existentes");
            }

            if (!columnExists(connection, "ap_protection_members", "joined_at")) {
                stmt.execute("ALTER TABLE ap_protection_members ADD COLUMN joined_at INTEGER DEFAULT 0");
                plugin.getLogger().info("Migracion: columna joined_at agregada a ap_protection_members");
            }

            if (!columnExists(connection, "ap_protections", "hologram_enabled")) {
                stmt.execute("ALTER TABLE ap_protections ADD COLUMN hologram_enabled INTEGER DEFAULT 0");
                plugin.getLogger().info("Migracion: columna hologram_enabled agregada a ap_protections");
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_rentals (" +
                    "region_id INT NOT NULL," +
                    "renter_uuid TEXT NOT NULL," +
                    "start_time INTEGER NOT NULL," +
                    "end_time INTEGER NOT NULL," +
                    "price REAL DEFAULT 0.00," +
                    "auto_renew INTEGER DEFAULT 0," +
                    "last_payment INTEGER DEFAULT 0," +
                    "duration_days INT NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (region_id, renter_uuid)," +
                    "FOREIGN KEY (region_id) REFERENCES ap_protections(id) ON DELETE CASCADE" +
                    ");");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_rentals_end_time ON ap_rentals(end_time)");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_block_log (" +
                    "log_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "region_id INT NOT NULL," +
                    "player_uuid TEXT NOT NULL," +
                    "old_type TEXT DEFAULT NULL," +
                    "new_type TEXT DEFAULT NULL," +
                    "block_data TEXT DEFAULT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "z INT NOT NULL," +
                    "timestamp INTEGER NOT NULL," +
                    "action TEXT NOT NULL" +
                    ");");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_log_region ON ap_block_log(region_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_log_player ON ap_block_log(player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_log_timestamp ON ap_block_log(timestamp)");

            stmt.execute("CREATE TABLE IF NOT EXISTS ap_flag_presets (" +
                    "preset_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "owner_uuid TEXT NOT NULL," +
                    "is_global INTEGER DEFAULT 0," +
                    "flag_data TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_presets_owner ON ap_flag_presets(owner_uuid)");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_presets_name_owner ON ap_flag_presets(name, owner_uuid)");

            if (!columnExists(connection, "ap_rentals", "duration_days")) {
                stmt.execute("ALTER TABLE ap_rentals ADD COLUMN duration_days INT NOT NULL DEFAULT 0");
                plugin.getLogger().info("Migracion: columna duration_days agregada a ap_rentals");
            }
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getDatabaseType() {
        return "sqlite";
    }

    @Override
    public SqlDialect getDialect() {
        return dialect;
    }
}
