package com.amplan.amplprotections.database;

public class MySQLDialect implements SqlDialect {

    @Override
    public String getAutoIncrementKeyword() {
        return "AUTO_INCREMENT";
    }

    @Override
    public String getUpsertSql(String table, String[] columns, String[] conflictColumns) {
        StringBuilder sb = new StringBuilder("REPLACE INTO ");
        sb.append(table).append(" (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(columns[i]);
        }
        sb.append(") VALUES (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getUpsertWithUpdateSql(String table, String[] insertColumns, String[] conflictColumns, String[] updateColumns) {
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(table).append(" (");
        for (int i = 0; i < insertColumns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(insertColumns[i]);
        }
        sb.append(") VALUES (");
        for (int i = 0; i < insertColumns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        sb.append(") ON DUPLICATE KEY UPDATE ");
        for (int i = 0; i < updateColumns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(updateColumns[i]).append(" = VALUES(").append(updateColumns[i]).append(")");
        }
        return sb.toString();
    }

    @Override
    public String getTimestampFunction() {
        return "UNIX_TIMESTAMP() * 1000";
    }

    @Override
    public String getBooleanType() {
        return "BOOLEAN";
    }

    @Override
    public String getTinyIntType() {
        return "TINYINT";
    }

    @Override
    public String getDecimalType() {
        return "DECIMAL(10,2)";
    }

    @Override
    public String getCheckColumnExistsSql(String tableName, String columnName) {
        return "SHOW COLUMNS FROM " + tableName + " LIKE '" + columnName + "'";
    }

    @Override
    public String getAddColumnAfterSql(String tableName, String columnName, String columnType, String afterColumn) {
        return "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType + " AFTER " + afterColumn;
    }

    @Override
    public String getModifyColumnSql(String tableName, String columnName, String newType) {
        return "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + newType;
    }

    @Override
    public String getCreateTableSuffix() {
        return "";
    }
}
