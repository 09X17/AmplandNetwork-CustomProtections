package com.amplan.amplprotections.preset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.FlagPermissionLevel;
import com.amplan.amplprotections.model.FlagPreset;

public class PresetManager {

    private final AmplProtections plugin;
    private final Map<String, FlagPreset> builtInPresets = new HashMap<>();
    private final Map<Integer, FlagPreset> customPresets = new ConcurrentHashMap<>();

    public PresetManager(AmplProtections plugin) {
        this.plugin = plugin;
        loadBuiltInPresets();
    }

    private void loadBuiltInPresets() {
        builtInPresets.clear();
        ConfigurationSection presetsSection = plugin.getConfig().getConfigurationSection("presets.built-in");
        if (presetsSection == null)
            return;

        for (String key : presetsSection.getKeys(false)) {
            FlagPreset preset = new FlagPreset(key, null, true);
            ConfigurationSection presetSection = presetsSection.getConfigurationSection(key);
            if (presetSection == null)
                continue;

            preset.setDescription(presetSection.getString("description", ""));

            ConfigurationSection flagsSection = presetSection.getConfigurationSection("flags");
            if (flagsSection != null) {
                for (String flagKey : flagsSection.getKeys(false)) {
                    String levelStr = Objects.requireNonNullElse(flagsSection.getString(flagKey), "NONE");
                    try {
                        FlagPermissionLevel level = FlagPermissionLevel.valueOf(levelStr.toUpperCase());
                        preset.setFlag(flagKey, level);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            builtInPresets.put(key, preset);
        }
    }

    public void reload() {
        customPresets.clear();
        loadBuiltInPresets();
        loadCustomPresets();
    }

    public void loadCustomPresets() {
        plugin.getProtectionManager().getProtectionDao().loadAllPresetsAsync().thenAccept(presets -> {
            if (presets != null) {
                for (FlagPreset preset : presets) {
                    customPresets.put(preset.getId(), preset);
                }
            }
        });
    }

    public FlagPreset getBuiltInPreset(String name) {
        return builtInPresets.get(name.toLowerCase());
    }

    public FlagPreset getCustomPreset(int id) {
        return customPresets.get(id);
    }

    public List<FlagPreset> getPlayerPresets(Player player) {
        List<FlagPreset> result = new ArrayList<>(builtInPresets.values());
        for (FlagPreset preset : customPresets.values()) {
            if (preset.getOwnerUuid().equals(player.getUniqueId())) {
                result.add(preset);
            }
        }
        return result;
    }

    public boolean createCustomPreset(Player player, String name, FlagPreset source) {
        if (builtInPresets.containsKey(name.toLowerCase()))
            return false;

        for (FlagPreset preset : customPresets.values()) {
            if (preset.getName().equalsIgnoreCase(name) && preset.getOwnerUuid().equals(player.getUniqueId())) {
                return false;
            }
        }

        FlagPreset newPreset = new FlagPreset(name, player.getUniqueId(), false);
        newPreset.setDescription("Custom preset by " + player.getName());
        for (Map.Entry<String, FlagPermissionLevel> entry : source.getFlags().entrySet()) {
            newPreset.setFlag(entry.getKey(), entry.getValue());
        }

        plugin.getProtectionManager().getProtectionDao().savePresetAsync(newPreset).thenAccept(id -> {
            if (id > 0) {
                newPreset.setId(id);
                customPresets.put(id, newPreset);
            }
        });

        return true;
    }

    public boolean deleteCustomPreset(Player player, int id) {
        FlagPreset preset = customPresets.get(id);
        if (preset == null || !preset.getOwnerUuid().equals(player.getUniqueId()))
            return false;

        customPresets.remove(id);
        plugin.getProtectionManager().getProtectionDao().deletePresetAsync(id);
        return true;
    }

    public boolean deleteCustomPreset(Player player, String name) {
        for (Map.Entry<Integer, FlagPreset> entry : customPresets.entrySet()) {
            if (entry.getValue().getName().equalsIgnoreCase(name)
                    && entry.getValue().getOwnerUuid().equals(player.getUniqueId())) {
                return deleteCustomPreset(player, entry.getKey());
            }
        }
        return false;
    }

    public Map<String, FlagPreset> getBuiltInPresets() {
        return Collections.unmodifiableMap(builtInPresets);
    }

    public Map<Integer, FlagPreset> getCustomPresets() {
        return Collections.unmodifiableMap(customPresets);
    }
}
