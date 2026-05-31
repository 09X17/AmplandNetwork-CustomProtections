package com.amplan.amplprotections.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigUpdater {

    private static final String LIST_MARKER = "- ";
    private static final Logger LOGGER = Logger.getLogger("AmplProtections");

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

            List<String> userLines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
            List<String> defaultKeyOrder = defaultKeyOrder(defaultLines);

            boolean changed = false;

            Set<String> newKeys = findNewKeys(defaultConfig, userConfig, "");
            if (!newKeys.isEmpty()) {
                LOGGER.info(fileName + " - Keys nuevas: " + newKeys);
            }

            for (String key : newKeys) {
                int defaultLineIdx = findKeyLineInDefault(defaultLines, key);
                if (defaultLineIdx == -1) continue;

                List<String> keyBlock = collectKeyBlock(defaultLines, defaultLineIdx);
                boolean isListItem = isListItem(defaultLines, defaultLineIdx);
                int insertIdx = findInsertionPoint(userLines, key, defaultKeyOrder, defaultLines);

                if (isListItem) {
                    String indent = getLineIndent(defaultLines.get(defaultLineIdx));
                    List<String> listItemBlock = new ArrayList<>();
                    for (String line : keyBlock) {
                        if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                            listItemBlock.add(line);
                        } else {
                            String trimmed = line.trim();
                            if (trimmed.startsWith(LIST_MARKER)) {
                                listItemBlock.add(line);
                            } else {
                                listItemBlock.add(indent + LIST_MARKER + trimmed);
                            }
                        }
                    }
                    userLines.addAll(insertIdx, listItemBlock);
                } else {
                    userLines.addAll(insertIdx, keyBlock);
                }
                changed = true;
            }

            changed |= syncTopLevelSections(userLines, defaultLines);

            if (changed) {
                Files.write(file.toPath(), userLines, StandardCharsets.UTF_8);
                LOGGER.info("Config actualizada: " + fileName);
            }
            return changed;

        } catch (Exception e) {
            LOGGER.warning("Error actualizando " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean syncTopLevelSections(List<String> userLines, List<String> defaultLines) {
        boolean changed = false;

        for (String topKey : defaultKeyOrder(defaultLines)) {
            int userIdx = findTopLevelKeyIndex(userLines, topKey);
            if (userIdx == -1) continue;

            int defaultIdx = findTopLevelKeyIndex(defaultLines, topKey);
            if (defaultIdx == -1) continue;

            List<String> userBlock = collectBlock(userLines, userIdx);
            List<String> defaultBlock = collectBlock(defaultLines, defaultIdx);

            List<String> newBlock = new ArrayList<>();
            int userLineIdx = 0;

            for (int d = 0; d < defaultBlock.size(); d++) {
                String defaultLine = defaultBlock.get(d);
                String defaultTrimmed = defaultLine.trim();

                if (defaultTrimmed.isEmpty() || defaultTrimmed.startsWith("#")) {
                    if (userLineIdx < userBlock.size()) {
                        String userTrimmed = userBlock.get(userLineIdx).trim();
                        if (userTrimmed.equals(defaultTrimmed) ||
                                userTrimmed.startsWith("#") || userTrimmed.isEmpty()) {
                            newBlock.add(userBlock.get(userLineIdx));
                            userLineIdx++;
                        } else {
                            newBlock.add(defaultLine);
                        }
                    } else {
                        newBlock.add(defaultLine);
                    }
                    continue;
                }

                String defaultKey = getKeyFromLine(defaultTrimmed);
                if (defaultKey == null) {
                    if (userLineIdx < userBlock.size()) {
                        newBlock.add(userBlock.get(userLineIdx));
                        userLineIdx++;
                    } else {
                        newBlock.add(defaultLine);
                    }
                    continue;
                }

                int blockIdx = findLineIndexWithKey(userBlock, userLineIdx, defaultKey);
                if (blockIdx != -1) {
                    newBlock.add(userBlock.get(blockIdx));
                    userLineIdx = blockIdx + 1;
                } else {
                    newBlock.add(defaultLine);
                    changed = true;
                }
            }

            int userEnd = findTopLevelKeyEnd(userLines, topKey);
            if (userEnd != -1) {
                userLines.subList(userIdx, userEnd + 1).clear();
                userLines.addAll(userIdx, newBlock);
            }
        }

        return changed;
    }

    private static List<String> defaultKeyOrder(List<String> defaultLines) {
        List<String> order = new ArrayList<>();
        for (String line : defaultLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (countIndent(line) == 0 && trimmed.contains(":")) {
                String key = trimmed.split(":")[0].trim();
                if (!key.isEmpty()) order.add(key);
            }
        }
        return order;
    }

    private static List<String> collectBlock(List<String> lines, int startIdx) {
        List<String> block = new ArrayList<>();
        block.add(lines.get(startIdx));
        int keyIndent = countIndent(lines.get(startIdx));
        for (int i = startIdx + 1; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty()) {
                block.add(lines.get(i));
                continue;
            }
            if (countIndent(lines.get(i)) > keyIndent) {
                block.add(lines.get(i));
            } else {
                break;
            }
        }
        return block;
    }

    private static String getKeyFromLine(String trimmed) {
        if (!trimmed.contains(":") || trimmed.startsWith("- ")) return null;
        return trimmed.split(":")[0].trim();
    }

    private static int findLineIndexWithKey(List<String> block, int fromIdx, String key) {
        for (int i = fromIdx; i < block.size(); i++) {
            String trimmed = block.get(i).trim();
            if (trimmed.startsWith(key + ":")) return i;
        }
        return -1;
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

    private static int findKeyLineInDefault(List<String> defaultLines, String key) {
        String[] parts = key.split("\\.");
        int targetIndent = (parts.length - 1) * 2;
        String targetKeyName = parts[parts.length - 1];

        for (int i = 0; i < defaultLines.size(); i++) {
            String trimmed = defaultLines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;
            int indent = countIndent(defaultLines.get(i));
            if (indent != targetIndent) continue;
            String lineKey = trimmed.split(":")[0].trim();
            if (lineKey.equals(targetKeyName)) {
                if (parts.length > 1) {
                    String parentKey = parts[parts.length - 2];
                    if (!hasParentKey(defaultLines, i, parentKey)) continue;
                }
                return i;
            }
        }
        return -1;
    }

    private static boolean hasParentKey(List<String> lines, int lineIndex, String parentKey) {
        int lineIndent = countIndent(lines.get(lineIndex));
        for (int i = lineIndex - 1; i >= 0; i--) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;
            int indent = countIndent(lines.get(i));
            if (indent < lineIndent) {
                String key = trimmed.split(":")[0].trim();
                return key.equals(parentKey);
            }
        }
        return false;
    }

    private static boolean isListItem(List<String> lines, int keyIndex) {
        for (int i = keyIndex - 1; i >= 0; i--) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;
            return trimmed.startsWith(LIST_MARKER);
        }
        return false;
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

    private static int findInsertionPoint(List<String> userLines, String key,
            List<String> defaultKeyOrder, List<String> defaultLines) {
        String[] parts = key.split("\\.");

        if (parts.length == 1) {
            int keyPos = defaultKeyOrder.indexOf(key);
            if (keyPos > 0) {
                String prevSibling = defaultKeyOrder.get(keyPos - 1);
                int prevEnd = findTopLevelKeyEnd(userLines, prevSibling);
                if (prevEnd != -1) return prevEnd + 1;
            }
            return userLines.size();
        }

        String parentPath = String.join(".", Arrays.copyOf(parts, parts.length - 1));

        String prevSibling = getPreviousSiblingInDefault(defaultLines, key);
        if (prevSibling != null) {
            String prevFull = parentPath + "." + prevSibling;
            int prevEnd = findNestedKeyEnd(userLines, prevFull);
            if (prevEnd != -1) return prevEnd + 1;
        }

        int parentLine = findKeyLineInUser(userLines, parentPath);
        if (parentLine != -1) {
            int parentIndent = countIndent(userLines.get(parentLine));
            int lastChild = -1;
            for (int i = parentLine + 1; i < userLines.size(); i++) {
                String trimmed = userLines.get(i).trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("#")) {
                    lastChild = i;
                    continue;
                }
                int lineIndent = countIndent(userLines.get(i));
                if (lineIndent > parentIndent) {
                    lastChild = i;
                } else {
                    break;
                }
            }
            return lastChild != -1 ? lastChild + 1 : parentLine + 1;
        }

        return userLines.size();
    }

    private static String getPreviousSiblingInDefault(List<String> defaultLines, String key) {
        String[] parts = key.split("\\.");
        int targetIndent = (parts.length - 1) * 2;
        String targetKeyName = parts[parts.length - 1];
        String parentKey = parts.length > 1 ? parts[parts.length - 2] : null;

        String prevSibling = null;
        for (int i = 0; i < defaultLines.size(); i++) {
            String trimmed = defaultLines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;
            int indent = countIndent(defaultLines.get(i));
            if (indent != targetIndent) continue;

            String lineKey = trimmed.split(":")[0].trim();

            if (parentKey != null && !hasParentKey(defaultLines, i, parentKey)) continue;

            if (lineKey.equals(targetKeyName)) return prevSibling;
            prevSibling = lineKey;
        }
        return null;
    }

    private static int findTopLevelKeyIndex(List<String> lines, String key) {
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;
            if (countIndent(lines.get(i)) != 0) continue;
            String lineKey = trimmed.split(":")[0].trim();
            if (lineKey.equals(key)) return i;
        }
        return -1;
    }

    private static int findTopLevelKeyEnd(List<String> lines, String key) {
        boolean found = false;
        int keyIndent = -1;
        int lastEnd = -1;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty()) {
                if (found) lastEnd = i;
                continue;
            }
            if (trimmed.startsWith("#")) {
                if (found) lastEnd = i;
                continue;
            }
            int indent = countIndent(lines.get(i));

            if (!found) {
                if (indent == 0) {
                    String lineKey = trimmed.split(":")[0].trim();
                    if (lineKey.equals(key)) {
                        found = true;
                        keyIndent = indent;
                        lastEnd = i;
                    }
                }
            } else {
                if (indent > keyIndent) {
                    lastEnd = i;
                } else {
                    return lastEnd;
                }
            }
        }
        return found ? lastEnd : -1;
    }

    private static int findNestedKeyEnd(List<String> lines, String key) {
        String[] parts = key.split("\\.");
        int targetIndent = (parts.length - 1) * 2;
        String targetKeyName = parts[parts.length - 1];
        String parentKey = parts.length > 1 ? parts[parts.length - 2] : null;

        boolean found = false;
        int lastEnd = -1;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty()) {
                if (found) lastEnd = i;
                continue;
            }
            if (trimmed.startsWith("#")) {
                if (found) lastEnd = i;
                continue;
            }
            int indent = countIndent(lines.get(i));

            if (!found) {
                if (indent == targetIndent) {
                    String lineKey = trimmed.split(":")[0].trim();
                    if (lineKey.equals(targetKeyName)) {
                        if (parentKey == null || hasParentKey(lines, i, parentKey)) {
                            found = true;
                            lastEnd = i;
                        }
                    }
                }
            } else {
                if (indent > targetIndent) {
                    lastEnd = i;
                } else {
                    return lastEnd;
                }
            }
        }
        return found ? lastEnd : -1;
    }

    private static int findKeyLineInUser(List<String> userLines, String key) {
        String[] parts = key.split("\\.");
        int targetIndent = (parts.length - 1) * 2;
        String targetKeyName = parts[parts.length - 1];
        String parentKey = parts.length > 1 ? parts[parts.length - 2] : null;

        for (int i = 0; i < userLines.size(); i++) {
            String trimmed = userLines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;
            int indent = countIndent(userLines.get(i));
            if (indent != targetIndent) continue;
            String lineKey = trimmed.split(":")[0].trim();
            if (lineKey.equals(targetKeyName)) {
                if (parentKey == null || hasParentKey(userLines, i, parentKey)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String getLineIndent(String line) {
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') sb.append(c);
            else break;
        }
        return sb.toString();
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

    public static void loadOrCreateConfig(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        } else {
            updateConfig(plugin, fileName);
        }
    }
}
