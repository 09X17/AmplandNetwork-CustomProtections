package com.amplan.amplprotections.language;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.amplan.amplprotections.AmplProtections;

public class LanguageManager {

    private final AmplProtections plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private String defaultLanguage;

    public LanguageManager(AmplProtections plugin) {
        this.plugin = plugin;
        this.defaultLanguage = plugin.getConfig().getString("language", "es");
        loadLanguages();
    }

    private void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String[] supportedLanguages = {"en", "es"};
        for (String lang : supportedLanguages) {
            File langFile = new File(langFolder, lang + ".yml");
            if (!langFile.exists()) {
                plugin.saveResource("lang/" + lang + ".yml", false);
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            languages.put(lang, config);
        }

        if (!languages.containsKey(defaultLanguage)) {
            defaultLanguage = "en";
        }
    }

    public void reload() {
        defaultLanguage = plugin.getConfig().getString("language", "es");
        loadLanguages();
    }

    public String getString(String key) {
        return getString(key, defaultLanguage);
    }

    public String getString(String key, String language) {
        FileConfiguration config = languages.get(language);
        if (config == null) {
            config = languages.get("en");
        }
        if (config == null) {
            return key;
        }
        String value = config.getString(key);
        if (value == null) {
            FileConfiguration fallback = languages.get("en");
            if (fallback != null) {
                value = fallback.getString(key, key);
            } else {
                value = key;
            }
        }
        return value;
    }

    public String getPrefixed(String key) {
        String prefix = getString("prefix");
        String msg = getString(key);
        return msg.replace("%prefix%", prefix);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String language) {
        if (languages.containsKey(language)) {
            this.defaultLanguage = language;
        }
    }

    public boolean isLanguageAvailable(String language) {
        return languages.containsKey(language);
    }
}
