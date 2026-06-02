package com.amplan.amplprotections.config;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.utils.ConfigUpdater;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MenuConfigManager {

    private final AmplProtections plugin;
    private final Map<String, FileConfiguration> menuConfigs = new HashMap<>();

    private static final String[] MENU_FILES = {
        "menus/main-menu.yml",
        "menus/members-menu.yml",
        "menus/global-menu.yml",
        "menus/list-menu.yml",
        "menus/buy-menu.yml",
        "menus/merge-menu.yml",
        "menus/preset-menu.yml",
        "menus/rental-menu.yml",
        "menus/rollback-menu.yml",
        "menus/bulk-manage-menu.yml"
    };

    public MenuConfigManager(AmplProtections plugin) {
        this.plugin = plugin;
    }

    public void loadAllMenus() {
        File menusDir = new File(plugin.getDataFolder(), "menus");
        if (!menusDir.exists()) {
            menusDir.mkdirs();
        }

        for (String fileName : MENU_FILES) {
            ConfigUpdater.loadOrCreateConfig(plugin, fileName);

            File menuFile = new File(plugin.getDataFolder(), fileName);
            FileConfiguration config = YamlConfiguration.loadConfiguration(menuFile);
            String key = fileName.replace("menus/", "").replace(".yml", "");
            menuConfigs.put(key, config);
        }
    }

    public void reloadAllMenus() {
        menuConfigs.clear();
        loadAllMenus();
    }

    public FileConfiguration getMenuConfig(String menuName) {
        return menuConfigs.getOrDefault(menuName, new YamlConfiguration());
    }

    public FileConfiguration getMainMenu() {
        return getMenuConfig("main-menu");
    }

    public FileConfiguration getMembersMenu() {
        return getMenuConfig("members-menu");
    }

    public FileConfiguration getGlobalMenu() {
        return getMenuConfig("global-menu");
    }

    public FileConfiguration getListMenu() {
        return getMenuConfig("list-menu");
    }

    public FileConfiguration getBuyMenu() {
        return getMenuConfig("buy-menu");
    }

    public FileConfiguration getMergeMenu() {
        return getMenuConfig("merge-menu");
    }

    public FileConfiguration getPresetMenu() {
        return getMenuConfig("preset-menu");
    }

    public FileConfiguration getRentalMenu() {
        return getMenuConfig("rental-menu");
    }

    public FileConfiguration getRollbackMenu() {
        return getMenuConfig("rollback-menu");
    }
}
