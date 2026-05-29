package com.amplan.amplprotections.hologram;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.ProtectionRegion;

public class HologramManager {

    private final AmplProtections plugin;
    private final Map<Integer, TextDisplay> activeHolograms = new HashMap<>();
    private BukkitTask updateTask;
    private boolean enabled;
    private int updateIntervalTicks;
    private String contentTemplate;
    private double heightOffset;
    private int visibleDistance;

    public HologramManager(AmplProtections plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection holoSection = plugin.getConfig().getConfigurationSection("holograms");
        if (holoSection == null) {
            enabled = true;
            updateIntervalTicks = 20;
            contentTemplate = "<white>%owner%</white> | <gray>%size%</gray>";
            heightOffset = 2.5;
            visibleDistance = 32;
            return;
        }

        enabled = holoSection.getBoolean("enabled", true);
        updateIntervalTicks = holoSection.getInt("update-interval-ticks", 20);
        contentTemplate = holoSection.getString("content", "<white>%owner%</white> | <gray>%size%</gray>");
        heightOffset = holoSection.getDouble("height-offset", 2.5);
        visibleDistance = holoSection.getInt("visible-distance", 32);
    }

    public void reload() {
        stopAll();
        loadConfig();
        if (enabled) {
            startUpdateTask();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void startUpdateTask() {
        if (updateTask != null) updateTask.cancel();
        if (!enabled) return;

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllHolograms, 0L, updateIntervalTicks);
    }

    public void spawnHologram(ProtectionRegion region) {
        if (!enabled) return;

        World world = Bukkit.getWorld(region.getWorldName());
        if (world == null) return;

        Location holoLoc = new Location(world, region.getBlockX() + 0.5, region.getBlockY() + heightOffset, region.getBlockZ() + 0.5);
        TextDisplay display = world.spawn(holoLoc, TextDisplay.class);
        display.setBillboard(Display.Billboard.VERTICAL);
        display.setSeeThrough(true);
        display.setViewRange((float) visibleDistance);
        display.setTransformation(new org.bukkit.util.Transformation(
                new org.joml.Vector3f(0, 0, 0),
                new org.joml.Quaternionf(),
                new org.joml.Vector3f(1, 1, 1),
                new org.joml.Quaternionf()
        ));

        updateHologramText(display, region);
        activeHolograms.put(region.getDatabaseId(), display);
    }

    public void removeHologram(ProtectionRegion region) {
        TextDisplay display = activeHolograms.remove(region.getDatabaseId());
        if (display != null) {
            display.remove();
        }
    }

    public boolean toggleHologram(ProtectionRegion region) {
        if (activeHolograms.containsKey(region.getDatabaseId())) {
            removeHologram(region);
            return false;
        } else {
            spawnHologram(region);
            return true;
        }
    }

    public boolean hasHologram(ProtectionRegion region) {
        return activeHolograms.containsKey(region.getDatabaseId());
    }

    private void updateAllHolograms() {
        for (Map.Entry<Integer, TextDisplay> entry : activeHolograms.entrySet()) {
            ProtectionRegion region = plugin.getProtectionManager().getAllRegions().stream()
                    .filter(r -> r.getDatabaseId() == entry.getKey())
                    .findFirst()
                    .orElse(null);
            if (region != null) {
                updateHologramText(entry.getValue(), region);
            }
        }
    }

    private void updateHologramText(TextDisplay display, ProtectionRegion region) {
        int radius = region.getRadius();
        String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);
        int membersOnline = (int) region.getMembers().keySet().stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .count();
        int totalMembers = region.getMembers().size() + 1;

        String text = contentTemplate
                .replace("%owner%", region.getOwnerName())
                .replace("%size%", size)
                .replace("%members%", String.valueOf(totalMembers))
                .replace("%members_online%", String.valueOf(membersOnline))
                .replace("%land_id%", region.getLandId())
                .replace("%type%", region.getTypeId())
                .replace("%x%", String.valueOf(region.getBlockX()))
                .replace("%y%", String.valueOf(region.getBlockY()))
                .replace("%z%", String.valueOf(region.getBlockZ()));

        display.text(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(text));
    }

    public void stopAll() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (TextDisplay display : activeHolograms.values()) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        activeHolograms.clear();
    }
}
