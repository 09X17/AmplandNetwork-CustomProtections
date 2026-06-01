package com.amplan.amplprotections.command;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.database.DatabaseConnection;
import com.amplan.amplprotections.database.DatabaseFactory;
import com.amplan.amplprotections.database.MigrationManager;
import com.amplan.amplprotections.database.MigrationManager.MigrationResult;
import com.amplan.amplprotections.database.MySQLConnection;
import com.amplan.amplprotections.database.SQLiteConnection;
import com.amplan.amplprotections.utils.MessageUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class MigrateCommand implements CommandExecutor, TabCompleter {

    private final AmplProtections plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MigrateCommand(AmplProtections plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("amplprotections.admin")) {
            sender.sendMessage(mm.deserialize(MessageUtils.lang("messages.no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize("<red>Uso: /aprot migrate <sqlite|mysql>"));
            return true;
        }

        String targetType = args[0].toLowerCase();
        if (!targetType.equals("sqlite") && !targetType.equals("mysql")) {
            sender.sendMessage(mm.deserialize("<red>Tipo de base de datos invalido. Usa 'sqlite' o 'mysql'."));
            return true;
        }

        DatabaseConnection currentDb = plugin.getDatabaseConnection();
        String currentType = currentDb.getDatabaseType();

        if (currentType.equals(targetType)) {
            sender.sendMessage(mm.deserialize("<yellow>Ya estas usando " + targetType.toUpperCase() + ". No se necesita migracion."));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(mm.deserialize("<yellow>Iniciando migracion de " + currentType.toUpperCase() + " a " + targetType.toUpperCase() + "..."));
        }

        sender.sendMessage(mm.deserialize("<yellow>Migrando de " + currentType.toUpperCase() + " a " + targetType.toUpperCase() + "... Esto puede tardar un momento."));

        DatabaseConnection targetDb = switch (targetType) {
            case "mysql" -> new MySQLConnection(plugin);
            default -> new SQLiteConnection(plugin);
        };

        MigrationManager migrationManager = new MigrationManager(plugin);
        MigrationResult result = migrationManager.migrate(currentDb, targetDb);

        if (result.isSuccess()) {
            plugin.getConfig().set("database.type", targetType);
            plugin.saveConfig();

            plugin.getDatabaseConnection().disconnect();
            DatabaseConnection newDb = DatabaseFactory.createDatabase(plugin);
            if (newDb.connect()) {
                plugin.setDatabaseConnection(newDb);
                plugin.getProtectionManager().clearAllRegions();
                plugin.getProtectionManager().loadRegionsFromDatabase();
                plugin.getPresetManager().reload();
                if (plugin.getRentalManager() != null) {
                    plugin.getRentalManager().reload();
                }
            } else {
                sender.sendMessage(mm.deserialize("<red>Migracion completada pero no se pudo reconectar. Reinicia el servidor."));
                return true;
            }

            sender.sendMessage(mm.deserialize("<green>Migracion completada exitosamente!"));
            sender.sendMessage(mm.deserialize("<green>Registros migrados: " + result.getTotalRecords()));
            for (var entry : result.getTableCounts().entrySet()) {
                sender.sendMessage(mm.deserialize("  <gray>- " + entry.getKey() + ": " + entry.getValue() + " registros"));
            }
            sender.sendMessage(mm.deserialize("<green>Config actualizada a database.type: " + targetType));
        } else {
            sender.sendMessage(mm.deserialize("<red>Error durante la migracion: " + result.getError()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("sqlite", "mysql").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
