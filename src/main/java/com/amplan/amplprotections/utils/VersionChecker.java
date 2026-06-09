package com.amplan.amplprotections.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.amplan.amplprotections.AmplProtections;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class VersionChecker implements Listener {

    private static final String MODRINTH_PROJECT_ID = "sL5AiZpx";

    private final AmplProtections plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private String latestVersion;
    private boolean isUpdateAvailable;
    private final Set<UUID> notifiedPlayers = new HashSet<>();

    public VersionChecker(AmplProtections plugin) {
        this.plugin = plugin;
        this.isUpdateAvailable = false;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String currentVersion = plugin.getPluginMeta().getVersion();
                String apiUrl = "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_ID + "/version";

                URL url = URI.create(apiUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "AmplProtections/" + currentVersion);

                if (connection.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        JsonArray versions = JsonParser.parseString(response.toString()).getAsJsonArray();
                        if (!versions.isEmpty()) {
                            JsonObject latest = versions.get(0).getAsJsonObject();
                            latestVersion = latest.get("version_number").getAsString();

                            if (isVersionNewer(latestVersion, currentVersion)) {
                                isUpdateAvailable = true;
                                final String cv = currentVersion; 
                                plugin.getLogger().log(Level.INFO, () -> """
                                        ===========================================
                                        A new version of AmplProtections is available!
                                        Current version: %s
                                        Latest version: %s
                                        Download at: https://modrinth.com/plugin/%s
                                        ===========================================""".formatted(cv, latestVersion, MODRINTH_PROJECT_ID));
                            }
                        }
                    }
                }
                connection.disconnect();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates", e);
            }
        });
    }

    private boolean isVersionNewer(String latest, String current) {
        if (latest == null || current == null) return false;

        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestNum = i < latestParts.length ? parseInt(latestParts[i]) : 0;
            int currentNum = i < currentParts.length ? parseInt(currentParts[i]) : 0;

            if (latestNum > currentNum) return true;
            if (latestNum < currentNum) return false;
        }
        return false;
    }

    private int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isUpdateAvailable && event.getPlayer().hasPermission("amplprotections.admin.notify")) {
            UUID playerId = event.getPlayer().getUniqueId();
            if (!notifiedPlayers.contains(playerId)) {
                notifiedPlayers.add(playerId);
                Player player = event.getPlayer();
                player.sendMessage(mm.deserialize("<yellow>[AmplProtections]</yellow> <green>A new version is available! (<white>" + latestVersion + "</white>)</green>"));
                player.sendMessage(mm.deserialize("<yellow>[AmplProtections]</yellow> <gray>Download: https://modrinth.com/plugin/" + MODRINTH_PROJECT_ID + "</gray>"));
            }
        }
    }
}