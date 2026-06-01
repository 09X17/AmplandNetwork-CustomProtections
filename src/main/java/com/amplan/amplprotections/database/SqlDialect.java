package com.amplan.amplprotections.database;

public interface SqlDialect {

    String getAutoIncrementKeyword();

    String getUpsertSql(String table, String[] columns, String[] conflictColumns);

    String getUpsertWithUpdateSql(String table, String[] insertColumns, String[] conflictColumns, String[] updateColumns);

    String getTimestampFunction();

    String getBooleanType();

    String getTinyIntType();

    String getDecimalType();

    String getCheckColumnExistsSql(String tableName, String columnName);

    String getAddColumnAfterSql(String tableName, String columnName, String columnType, String afterColumn);

    String getModifyColumnSql(String tableName, String columnName, String newType);

    String getCreateTableSuffix();

    default String buildCreateTableSuffix() {
        return getCreateTableSuffix();
    }
}
