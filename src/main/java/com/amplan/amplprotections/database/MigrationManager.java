package com.amplan.amplprotections.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.amplan.amplprotections.AmplProtections;

public class MigrationManager {

    private final AmplProtections plugin;

    public MigrationManager(AmplProtections plugin) {
        this.plugin = plugin;
    }

    public MigrationResult migrate(DatabaseConnection source, DatabaseConnection target) {
        MigrationResult result = new MigrationResult();
        result.setSource(source.getDatabaseType());
        result.setTarget(target.getDatabaseType());

        try {
            if (!target.connect()) {
                result.setSuccess(false);
                result.setError("No se pudo conectar a la base de datos target");
                return result;
            }

            try (Connection sourceConn = source.getConnection();
                 Connection targetConn = target.getConnection()) {

                target.createTables(targetConn);

                List<String> tables = List.of(
                        "ap_protections", "ap_protection_members", "ap_protection_flags",
                        "ap_protection_merges", "ap_rentals", "ap_block_log", "ap_flag_presets"
                );

                for (String table : tables) {
                    int count = migrateTable(sourceConn, targetConn, target, table);
                    result.addTableCount(table, count);
                }

                resetAutoIncrement(targetConn, target.getDatabaseType());

                result.setSuccess(true);
            }

        } catch (SQLException e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error durante la migracion", e);
        } finally {
            target.disconnect();
        }

        return result;
    }

    private int migrateTable(Connection sourceConn, Connection targetConn, DatabaseConnection targetDb, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement stmt = sourceConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " LIMIT 1")) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columns.add(metaData.getColumnName(i));
            }
        }

        if (columns.isEmpty()) {
            return 0;
        }

        String placeholders = String.join(", ", columns.stream().map(c -> "?").toList());
        String columnList = String.join(", ", columns);
        String[] columnsArray = columns.toArray(new String[0]);
        String insertSql = targetDb.getDialect().getUpsertSql(table, columnsArray, new String[]{columnsArray[0]});

        int count = 0;
        try (Statement stmt = sourceConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {

            targetConn.setAutoCommit(false);
            try (PreparedStatement ps = targetConn.prepareStatement(insertSql)) {
                while (rs.next()) {
                    for (int i = 0; i < columns.size(); i++) {
                        ps.setObject(i + 1, rs.getObject(i + 1));
                    }
                    ps.addBatch();
                    count++;

                    if (count % 500 == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
                targetConn.commit();
            } catch (SQLException e) {
                targetConn.rollback();
                throw e;
            } finally {
                targetConn.setAutoCommit(true);
            }
        }

        return count;
    }

    private void resetAutoIncrement(Connection conn, String dbType) throws SQLException {
        if (dbType.equals("mysql")) {
            try (Statement stmt = conn.createStatement()) {
                String[] tables = {"ap_protections", "ap_block_log", "ap_flag_presets"};
                for (String table : tables) {
                    stmt.execute("ALTER TABLE " + table + " AUTO_INCREMENT = 1");
                }
            }
        }
    }

    public static class MigrationResult {
        private boolean success;
        private String error;
        private String source;
        private String target;
        private final java.util.Map<String, Integer> tableCounts = new java.util.LinkedHashMap<>();

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public java.util.Map<String, Integer> getTableCounts() { return tableCounts; }

        public void addTableCount(String table, int count) {
            tableCounts.put(table, count);
        }

        public int getTotalRecords() {
            return tableCounts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }
}
