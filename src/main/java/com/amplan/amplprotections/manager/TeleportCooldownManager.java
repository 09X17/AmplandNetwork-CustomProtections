package com.amplan.amplprotections.manager;

import com.amplan.amplprotections.AmplProtections;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportCooldownManager {

    private final AmplProtections plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private long cooldownMillis;
    private String bypassPermission;

    public TeleportCooldownManager(AmplProtections plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection tpSection = plugin.getConfig().getConfigurationSection("teleport");
        if (tpSection == null) {
            cooldownMillis = 30000;
            bypassPermission = "amplprotections.cooldown.bypass";
            return;
        }

        int seconds = tpSection.getInt("cooldown-seconds", 30);
        cooldownMillis = seconds * 1000L;
        bypassPermission = tpSection.getString("bypass-permission", "amplprotections.cooldown.bypass");
    }

    public void reload() {
        loadConfig();
    }

    public boolean hasBypass(Player player) {
        return player.hasPermission(bypassPermission);
    }

    public boolean isOnCooldown(Player player) {
        if (hasBypass(player)) return false;
        if (cooldownMillis <= 0) return false;

        Long lastTeleport = cooldowns.get(player.getUniqueId());
        if (lastTeleport == null) return false;

        return (System.currentTimeMillis() - lastTeleport) < cooldownMillis;
    }

    public long getRemainingSeconds(Player player) {
        Long lastTeleport = cooldowns.get(player.getUniqueId());
        if (lastTeleport == null) return 0;

        long elapsed = System.currentTimeMillis() - lastTeleport;
        long remaining = cooldownMillis - elapsed;
        return Math.max(0, remaining / 1000L + 1);
    }

    public void setCooldown(Player player) {
        if (hasBypass(player)) return;
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void removeCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}
