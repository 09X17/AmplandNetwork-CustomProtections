package com.amplan.amplprotections.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigUpdater {

    /**
     * Hace merge de un archivo YAML default (del JAR) con el existente del usuario.
     * Preserva todos los comentarios y formato del usuario.
     * Keys nuevas se insertan al final de su sección padre con sus comentarios.
     */
    public static boolean updateConfig(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) return false;

        try {
            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(file);

            List<String> defaultLines;
            try (InputStream stream = plugin.getResource(fileName)) {
                if (stream == null) return false;
                defaultLines = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.toList());
            }

            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new StringReader(String.join("\n", defaultLines)));

            Set<String> newKeys = findNewKeys(defaultConfig, userConfig, "");
            if (newKeys.isEmpty()) return false;

            List<String> userLines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

            for (String key : newKeys) {
                int defaultLineIdx = findKeyLine(defaultLines, key);
                if (defaultLineIdx == -1) continue;

                List<String> keyBlock = collectKeyBlock(defaultLines, defaultLineIdx);
                int insertIdx = findInsertionPoint(userLines, key);
                userLines.addAll(insertIdx, keyBlock);
            }

            Files.write(file.toPath(), userLines, StandardCharsets.UTF_8);
            plugin.getLogger().info("Config actualizada (comentarios preservados): " + fileName);
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Error actualizando " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    private static Set<String> findNewKeys(ConfigurationSection defaults, ConfigurationSection target, String path) {
        Set<String> newKeys = new LinkedHashSet<>();
        for (String key : defaults.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            Object defaultValue = defaults.get(key);
            if (defaultValue instanceof ConfigurationSection defaultSection) {
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                if (targetSection == null) {
                    newKeys.add(fullPath);
                } else {
                    newKeys.addAll(findNewKeys(defaultSection, targetSection, fullPath));
                }
            } else {
                if (!target.contains(key)) {
                    newKeys.add(fullPath);
                }
            }
        }
        return newKeys;
    }

    private static int findKeyLine(List<String> lines, String key) {
        String[] parts = key.split("\\.");
        int targetIndent = (parts.length - 1) * 2;
        String targetKeyName = parts[parts.length - 1];

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;
            int indent = countIndent(lines.get(i));
            if (indent != targetIndent) continue;
            String lineKey = trimmed.split(":")[0].trim();
            if (lineKey.equals(targetKeyName)) return i;
        }
        return -1;
    }

    private static List<String> collectKeyBlock(List<String> lines, int startIndex) {
        List<String> block = new ArrayList<>();

        int i = startIndex - 1;
        while (i >= 0) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                block.add(0, lines.get(i));
                i--;
            } else break;
        }

        block.add(lines.get(startIndex));

        int keyIndent = countIndent(lines.get(startIndex));
        i = startIndex + 1;
        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                block.add(line);
                i++;
                continue;
            }
            if (countIndent(line) > keyIndent) {
                block.add(line);
                i++;
            } else break;
        }

        return block;
    }

    private static int findInsertionPoint(List<String> userLines, String key) {
        String[] parts = key.split("\\.");
        if (parts.length == 1) return userLines.size();

        String parentPath = String.join(".", Arrays.copyOf(parts, parts.length - 1));
        int parentEnd = findSectionEnd(userLines, parentPath);
        return parentEnd == -1 ? userLines.size() : parentEnd + 1;
    }

    private static int findSectionEnd(List<String> lines, String sectionPath) {
        String[] parts = sectionPath.split("\\.");
        int lastSeenLine = -1;
        int sectionIndent = (parts.length - 1) * 2;
        boolean inSection = false;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                if (inSection) lastSeenLine = i;
                continue;
            }
            int lineIndent = countIndent(lines.get(i));

            if (!inSection) {
                if (lineIndent == sectionIndent) {
                    String lineKey = trimmed.split(":")[0].trim();
                    if (lineKey.equals(parts[parts.length - 1])) {
                        inSection = true;
                        lastSeenLine = i;
                    }
                }
            } else {
                if (lineIndent > sectionIndent) {
                    lastSeenLine = i;
                } else break;
            }
        }
        return lastSeenLine;
    }

    private static int countIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 2;
            else break;
        }
        return count;
    }

    /**
     * Crea el archivo si no existe, y si existe, hace merge preservando comentarios.
     */
    public static void loadOrCreateConfig(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info("Archivo creado: " + fileName);
        } else {
            updateConfig(plugin, fileName);
        }
    }
}
