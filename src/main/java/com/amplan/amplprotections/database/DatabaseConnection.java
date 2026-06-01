package com.amplan.amplprotections.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

public interface DatabaseConnection {

    boolean connect();

    void disconnect();

    Connection getConnection() throws SQLException;

    ExecutorService getDbExecutor();

    void createTables(Connection connection) throws SQLException;

    String getDatabaseType();

    SqlDialect getDialect();
}
