package com.amplan.amplprotections.database;

import org.bukkit.configuration.file.FileConfiguration;

import com.amplan.amplprotections.AmplProtections;

public class DatabaseFactory {

    public static DatabaseConnection createDatabase(AmplProtections plugin) {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type", "sqlite").toLowerCase();

        return switch (type) {
            case "mysql" -> {
                plugin.getLogger().info("Iniciando conexion MySQL...");
                yield new MySQLConnection(plugin);
            }
            default -> {
                plugin.getLogger().info("Iniciando conexion SQLite (por defecto)...");
                yield new SQLiteConnection(plugin);
            }
        };
    }
}
