package com.amplan.amplprotections.database;

public class SQLiteDialect implements SqlDialect {

    @Override
    public String getAutoIncrementKeyword() {
        return "AUTOINCREMENT";
    }

    @Override
    public String getUpsertSql(String table, String[] columns, String[] conflictColumns) {
        StringBuilder sb = new StringBuilder("INSERT OR REPLACE INTO ");
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
        sb.append(") ON CONFLICT(");
        for (int i = 0; i < conflictColumns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(conflictColumns[i]);
        }
        sb.append(") DO UPDATE SET ");
        for (int i = 0; i < updateColumns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(updateColumns[i]).append(" = excluded.").append(updateColumns[i]);
        }
        return sb.toString();
    }

    @Override
    public String getTimestampFunction() {
        return null;
    }

    @Override
    public String getBooleanType() {
        return "INTEGER";
    }

    @Override
    public String getTinyIntType() {
        return "INTEGER";
    }

    @Override
    public String getDecimalType() {
        return "REAL";
    }

    @Override
    public String getCheckColumnExistsSql(String tableName, String columnName) {
        return "PRAGMA table_info(" + tableName + ")";
    }

    @Override
    public String getAddColumnAfterSql(String tableName, String columnName, String columnType, String afterColumn) {
        return "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType;
    }

    @Override
    public String getModifyColumnSql(String tableName, String columnName, String newType) {
        return null;
    }

    @Override
    public String getCreateTableSuffix() {
        return "";
    }
}
