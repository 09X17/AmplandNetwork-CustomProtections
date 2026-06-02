package com.amplan.amplprotections;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.amplan.amplprotections.command.AdminProtectionCommand;
import com.amplan.amplprotections.command.MigrateCommand;
import com.amplan.amplprotections.command.ProtectionCommand;
import com.amplan.amplprotections.config.MenuConfigManager;
import com.amplan.amplprotections.database.DatabaseConnection;
import com.amplan.amplprotections.database.DatabaseFactory;
import com.amplan.amplprotections.economy.EconomyManager;
import com.amplan.amplprotections.glow.GlowManager;
import com.amplan.amplprotections.hologram.HologramManager;
import com.amplan.amplprotections.language.LanguageManager;
import com.amplan.amplprotections.listener.BlockListener;
import com.amplan.amplprotections.listener.EntityListener;
import com.amplan.amplprotections.listener.ProtectionListener;
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.manager.TeleportCooldownManager;
import com.amplan.amplprotections.menu.ChatSearchListener;
import com.amplan.amplprotections.menu.MenuManager;
import com.amplan.amplprotections.preset.PresetManager;
import com.amplan.amplprotections.rental.RentalManager;
import com.amplan.amplprotections.rollback.BlockLogger;
import com.amplan.amplprotections.rollback.RollbackManager;
import com.amplan.amplprotections.utils.ConfigUpdater;

public final class AmplProtections extends JavaPlugin {

    private static AmplProtections instance;

    private File blocksFile;
    private FileConfiguration blocksConfig;
    private File adminMenuFile;
    private FileConfiguration adminMenuConfig;

    private DatabaseConnection databaseConnection;
    private ProtectionManager protectionManager;
    private MenuManager menuManager;
    private MenuConfigManager menuConfigManager;
    private EconomyManager economyManager;
    private TeleportCooldownManager teleportCooldownManager;
    private HologramManager hologramManager;
    private GlowManager glowManager;
    private PresetManager presetManager;
    private RentalManager rentalManager;
    private BlockLogger blockLogger;
    private RollbackManager rollbackManager;
    private ChatSearchListener chatSearchListener;
    private LanguageManager languageManager;
    private ProtectionListener protectionListener;

    @Override
    public void onEnable() {
        instance = this;

        ConfigUpdater.loadOrCreateConfig(this, "config.yml");
        ConfigUpdater.loadOrCreateConfig(this, "blocks.yml");
        ConfigUpdater.loadOrCreateConfig(this, "admin-menu.yml");

        reloadConfig();
        this.blocksFile = new File(getDataFolder(), "blocks.yml");
        this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
        this.adminMenuFile = new File(getDataFolder(), "admin-menu.yml");
        this.adminMenuConfig = YamlConfiguration.loadConfiguration(adminMenuFile);

        this.languageManager = new LanguageManager(this);

        this.menuManager = new MenuManager(this);
        this.menuConfigManager = new MenuConfigManager(this);
        this.menuConfigManager.loadAllMenus();
        this.economyManager = new EconomyManager(this);
        this.teleportCooldownManager = new TeleportCooldownManager(this);
        this.hologramManager = new HologramManager(this);
        this.glowManager = new GlowManager(this);
        this.presetManager = new PresetManager(this);
        this.databaseConnection = DatabaseFactory.createDatabase(this);

        getServer().getPluginManager().registerEvents(this.menuManager, this);
        this.chatSearchListener = new ChatSearchListener(this);
        getServer().getPluginManager().registerEvents(this.chatSearchListener, this);

        if (!databaseConnection.connect()) {
            getLogger().severe("No se pudo establecer conexion con la base de datos! Deshabilitando plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.protectionManager = new ProtectionManager(this);
        this.protectionManager.loadProtectionTypes();
        this.protectionManager.loadRegionsFromDatabase();
        this.presetManager.loadCustomPresets();
        this.hologramManager.startUpdateTask();

        if (getConfig().getBoolean("rental.enabled", true)) {
            this.rentalManager = new RentalManager(this);
            this.rentalManager.loadRentals();
            this.rentalManager.startExpiryCheckTask();
        }
        if (getConfig().getBoolean("rollback.enabled", true)) {
            this.blockLogger = new BlockLogger(this);
            this.blockLogger.startFlushTask();
            this.rollbackManager = new RollbackManager(this);
        }

        this.protectionListener = new ProtectionListener(this);
        getServer().getPluginManager().registerEvents(this.protectionListener, this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);

        AdminProtectionCommand adminCmd = new AdminProtectionCommand(this);

        var aprot = getCommand("aprot");
        if (aprot != null) {
            aprot.setExecutor(adminCmd);
            aprot.setTabCompleter(adminCmd);
        }

        MigrateCommand migrateCmd = new MigrateCommand(this);

        var migrate = getCommand("aprotmigrate");
        if (migrate != null) {
            migrate.setExecutor(migrateCmd);
            migrate.setTabCompleter(migrateCmd);
        }

        ProtectionCommand protectionCmd = new ProtectionCommand(this);

        var proteccion = getCommand("proteccion");
        if (proteccion != null) {
            proteccion.setExecutor(protectionCmd);
            proteccion.setTabCompleter(protectionCmd);
        }
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.amplan.amplprotections.hook.PlaceholderAPIHook(this).register();
            getLogger().info("PlaceholderAPI hook registrado correctamente.");
        }

        getLogger().info("¡AmplProtections ha sido habilitado correctamente!");
    }

    @Override
    public void onDisable() {
        if (rentalManager != null) {
            rentalManager.shutdown();
        }
        if (blockLogger != null) {
            blockLogger.shutdown();
        }
        if (chatSearchListener != null) {
            chatSearchListener.shutdown();
        }
        if (hologramManager != null) {
            hologramManager.stopAll();
        }
        if (glowManager != null) {
            glowManager.removeAll();
        }
        if (protectionManager != null) {
            protectionManager.saveAllRegionsSync();
        }
        if (databaseConnection != null) {
            databaseConnection.disconnect();
        }
    }

    public void reloadPluginFiles() {
        ConfigUpdater.updateConfig(this, "config.yml");
        ConfigUpdater.updateConfig(this, "blocks.yml");
        ConfigUpdater.updateConfig(this, "admin-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/main-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/members-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/global-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/list-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/buy-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/merge-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/preset-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/rental-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/rollback-menu.yml");
        ConfigUpdater.updateConfig(this, "menus/bulk-manage-menu.yml");
        reloadConfig();
        blocksConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "blocks.yml"));
        adminMenuConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "admin-menu.yml"));
        if (languageManager != null) {
            languageManager.reload();
        }
        if (protectionManager != null) {
            protectionManager.loadProtectionTypes();
            protectionManager.reloadFlags();
            protectionManager.clearOwnerNameCache();
        }
        if (protectionListener != null) {
            protectionListener.reload();
        }
        if (menuConfigManager != null) {
            menuConfigManager.reloadAllMenus();
        }
        if (economyManager != null) {
            economyManager.reload();
        }
        if (teleportCooldownManager != null) {
            teleportCooldownManager.reload();
        }
        if (hologramManager != null) {
            hologramManager.reload();
        }
        if (presetManager != null) {
            presetManager.reload();
        }
        if (rentalManager != null) {
            rentalManager.reload();
        }
        if (blockLogger != null) {
            blockLogger.reload();
        }
    }

    public static AmplProtections getInstance() {
        return instance;
    }

    public FileConfiguration getBlocksConfig() {
        return this.blocksConfig;
    }

    public FileConfiguration getAdminMenuConfig() {
        return this.adminMenuConfig;
    }

    public LanguageManager getLanguageManager() {
        return this.languageManager;
    }

    public DatabaseConnection getDatabaseConnection() {
        return this.databaseConnection;
    }

    public void setDatabaseConnection(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public ProtectionManager getProtectionManager() {
        return this.protectionManager;
    }

    public MenuManager getMenuManager() {
        return this.menuManager;
    }

    public MenuConfigManager getMenuConfigManager() {
        return this.menuConfigManager;
    }

    public EconomyManager getEconomyManager() {
        return this.economyManager;
    }

    public TeleportCooldownManager getTeleportCooldownManager() {
        return this.teleportCooldownManager;
    }

    public HologramManager getHologramManager() {
        return this.hologramManager;
    }

    public GlowManager getGlowManager() {
        return this.glowManager;
    }

    public PresetManager getPresetManager() {
        return this.presetManager;
    }

    public RentalManager getRentalManager() {
        return this.rentalManager;
    }

    public BlockLogger getBlockLogger() {
        return this.blockLogger;
    }

    public RollbackManager getRollbackManager() {
        return this.rollbackManager;
    }

    public ChatSearchListener getChatSearchListener() {
        return this.chatSearchListener;
    }
}
