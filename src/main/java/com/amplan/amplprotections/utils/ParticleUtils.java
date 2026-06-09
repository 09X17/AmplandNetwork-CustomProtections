package com.amplan.amplprotections.utils;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.ProtectionRegion;

public class ParticleUtils {

        public static void showBorderAsync(AmplProtections plugin, Player player, ProtectionRegion region) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                        World world = Bukkit.getWorld(region.getWorldName());
                        if (world == null || !player.getWorld().equals(world))
                                return;

                        int minX = region.getMinX();
                        int maxX = region.getMaxX();
                        int minZ = region.getMinZ();
                        int maxZ = region.getMaxZ();

                        Location playerLoc = player.getLocation();
                        if (playerLoc == null)
                                return;
                        double playerY = playerLoc.getY();
                        double lowerY = playerY - 1;
                        double upperY = playerY + 2;

                        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.0f);

                        for (int x = minX; x <= maxX; x++) {
                                player.spawnParticle(Particle.DUST, new Location(world, x + 0.5, lowerY, minZ + 0.5), 1,
                                                dustOptions);
                                player.spawnParticle(Particle.DUST, new Location(world, x + 0.5, lowerY, maxZ + 0.5), 1,
                                                dustOptions);
                                player.spawnParticle(Particle.DUST, new Location(world, x + 0.5, upperY, minZ + 0.5), 1,
                                                dustOptions);
                                player.spawnParticle(Particle.DUST, new Location(world, x + 0.5, upperY, maxZ + 0.5), 1,
                                                dustOptions);
                        }

                        for (int z = minZ; z <= maxZ; z++) {
                                player.spawnParticle(Particle.DUST, new Location(world, minX + 0.5, lowerY, z + 0.5), 1,
                                                dustOptions);
                                player.spawnParticle(Particle.DUST, new Location(world, maxX + 0.5, lowerY, z + 0.5), 1,
                                                dustOptions);
                                player.spawnParticle(Particle.DUST, new Location(world, minX + 0.5, upperY, z + 0.5), 1,
                                                dustOptions);
                                player.spawnParticle(Particle.DUST, new Location(world, maxX + 0.5, upperY, z + 0.5), 1,
                                                dustOptions);
                        }
                });
        }

        public static void showProtectionView(AmplProtections plugin, Player player, ProtectionRegion region) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                        World world = Bukkit.getWorld(region.getWorldName());
                        if (world == null || !player.getWorld().equals(world))
                                return;

                        int minX = region.getMinX();
                        int maxX = region.getMaxX();
                        int minZ = region.getMinZ();
                        int maxZ = region.getMaxZ();
                        Location loc = player.getLocation();
                        if (loc == null)
                                return;
                        int playerY = loc.getBlockY();
                        int minY = Math.max(world.getMinHeight(), playerY - 2);
                        int maxY = Math.min(world.getMaxHeight(), playerY + 8);

                        ConfigurationSection particleConfig = plugin.getConfig().getConfigurationSection("particles");
                        int durationTicks = particleConfig != null ? particleConfig.getInt("duration-ticks", 200) : 200;
                        int density = particleConfig != null ? particleConfig.getInt("density", 1) : 1;

                        String borderColorStr = particleConfig != null
                                        ? particleConfig.getString("border-color", "0,200,255")
                                        : "0,200,255";
                        String cornerColorStr = particleConfig != null
                                        ? particleConfig.getString("corner-color", "255,165,0")
                                        : "255,165,0";
                        float borderSize = particleConfig != null ? (float) particleConfig.getDouble("border-size", 1.2)
                                        : 1.2f;
                        float cornerSize = particleConfig != null ? (float) particleConfig.getDouble("corner-size", 1.5)
                                        : 1.5f;

                        ConfigurationSection pulseSection = particleConfig != null
                                        ? particleConfig.getConfigurationSection("pulse")
                                        : null;
                        float sizeMin = pulseSection != null ? (float) pulseSection.getDouble("size-min", 0.7) : 0.7f;
                        float sizeMax = pulseSection != null ? (float) pulseSection.getDouble("size-max", 1.3) : 1.3f;
                        int pulseInterval = pulseSection != null ? pulseSection.getInt("interval-ticks", 5) : 5;
                        boolean pulseEnabled = pulseSection != null && pulseSection.getBoolean("enabled", true);

                        Color borderColor = parseColor(borderColorStr);
                        Color cornerColor = parseColor(cornerColorStr);
                        Color glowColor = Color.fromRGB(255, 215, 0);

                        Particle.DustOptions borderDust = new Particle.DustOptions(borderColor, borderSize);
                        Particle.DustOptions cornerDust = new Particle.DustOptions(cornerColor, cornerSize);
                        Particle.DustOptions glowDust = new Particle.DustOptions(glowColor, 1.2f);

                        Location corner1 = new Location(world, minX + 0.5, minY, minZ + 0.5);
                        Location corner2 = new Location(world, maxX + 0.5, minY, minZ + 0.5);
                        Location corner3 = new Location(world, maxX + 0.5, minY, maxZ + 0.5);
                        Location corner4 = new Location(world, minX + 0.5, minY, maxZ + 0.5);

                        for (int y = minY; y <= maxY; y += density) {
                                player.spawnParticle(Particle.DUST, corner1.clone().add(0, y - minY, 0), 3, cornerDust);
                                player.spawnParticle(Particle.DUST, corner2.clone().add(0, y - minY, 0), 3, cornerDust);
                                player.spawnParticle(Particle.DUST, corner3.clone().add(0, y - minY, 0), 3, cornerDust);
                                player.spawnParticle(Particle.DUST, corner4.clone().add(0, y - minY, 0), 3, cornerDust);
                        }

                        for (int x = minX; x <= maxX; x += density) {
                                player.spawnParticle(Particle.DUST, new Location(world, x + 0.5, minY, minZ + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.DUST, new Location(world, x + 0.5, minY, maxZ + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.DUST, new Location(world, x + 0.5, maxY, minZ + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.DUST, new Location(world, x + 0.5, maxY, maxZ + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.GLOW,
                                                new Location(world, x + 0.5, minY + 0.5, minZ + 0.5), 1, 0.1f, 0, 0.1f,
                                                0.01f);
                                player.spawnParticle(Particle.GLOW,
                                                new Location(world, x + 0.5, minY + 0.5, maxZ + 0.5), 1, 0.1f, 0, 0.1f,
                                                0.01f);
                        }

                        for (int z = minZ; z <= maxZ; z += density) {
                                player.spawnParticle(Particle.DUST, new Location(world, minX + 0.5, minY, z + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.DUST, new Location(world, maxX + 0.5, minY, z + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.DUST, new Location(world, minX + 0.5, maxY, z + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.DUST, new Location(world, maxX + 0.5, maxY, z + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.GLOW,
                                                new Location(world, minX + 0.5, minY + 0.5, z + 0.5), 1, 0, 0.1f, 0.1f,
                                                0.01f);
                                player.spawnParticle(Particle.GLOW,
                                                new Location(world, maxX + 0.5, minY + 0.5, z + 0.5), 1, 0, 0.1f, 0.1f,
                                                0.01f);
                        }

                        for (int y = minY; y <= maxY; y += density) {
                                player.spawnParticle(Particle.DUST, new Location(world, minX + 0.5, y, minZ + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.DUST, new Location(world, maxX + 0.5, y, minZ + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.DUST, new Location(world, minX + 0.5, y, maxZ + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.DUST, new Location(world, maxX + 0.5, y, maxZ + 0.5), 1,
                                                borderDust);
                                player.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                                new Location(world, minX + 0.5, y, minZ + 0.5), 1, 0.1f, 0.1f, 0.1f,
                                                0.02f);
                                player.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                                new Location(world, maxX + 0.5, y, minZ + 0.5), 1, 0.1f, 0.1f, 0.1f,
                                                0.02f);
                                player.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                                new Location(world, minX + 0.5, y, maxZ + 0.5), 1, 0.1f, 0.1f, 0.1f,
                                                0.02f);
                                player.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                                new Location(world, maxX + 0.5, y, maxZ + 0.5), 1, 0.1f, 0.1f, 0.1f,
                                                0.02f);
                        }

                        for (int x = minX; x <= maxX; x += density) {
                                for (int y = minY + density; y < maxY; y += density * 3) {
                                        player.spawnParticle(Particle.END_ROD,
                                                        new Location(world, x + 0.5, y, minZ + 0.5), 1, 0, 0, 0, 0.02);
                                        player.spawnParticle(Particle.END_ROD,
                                                        new Location(world, x + 0.5, y, maxZ + 0.5), 1, 0, 0, 0, 0.02);
                                }
                        }

                        for (int z = minZ; z <= maxZ; z += density) {
                                for (int y = minY + density; y < maxY; y += density * 3) {
                                        player.spawnParticle(Particle.END_ROD,
                                                        new Location(world, minX + 0.5, y, z + 0.5), 1, 0, 0, 0, 0.02);
                                        player.spawnParticle(Particle.END_ROD,
                                                        new Location(world, maxX + 0.5, y, z + 0.5), 1, 0, 0, 0, 0.02);
                                }
                        }

                        player.spawnParticle(Particle.DUST, corner1.clone().add(0, 1, 0), 20, 0.5f, 0.5f, 0.5f,
                                        cornerDust);
                        player.spawnParticle(Particle.DUST, corner2.clone().add(0, 1, 0), 20, 0.5f, 0.5f, 0.5f,
                                        cornerDust);
                        player.spawnParticle(Particle.DUST, corner3.clone().add(0, 1, 0), 20, 0.5f, 0.5f, 0.5f,
                                        cornerDust);
                        player.spawnParticle(Particle.DUST, corner4.clone().add(0, 1, 0), 20, 0.5f, 0.5f, 0.5f,
                                        cornerDust);

                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, corner1.clone().add(0, 2, 0), 15, 0.3f, 0.5f,
                                        0.3f, 0.05);
                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, corner2.clone().add(0, 2, 0), 15, 0.3f, 0.5f,
                                        0.3f, 0.05);
                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, corner3.clone().add(0, 2, 0), 15, 0.3f, 0.5f,
                                        0.3f, 0.05);
                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, corner4.clone().add(0, 2, 0), 15, 0.3f, 0.5f,
                                        0.3f, 0.05);

                        Location centerLoc = new Location(world, region.getBlockX() + 0.5, region.getBlockY() + 0.5,
                                        region.getBlockZ() + 0.5);
                        if (!plugin.getConfig().getBoolean("glow.enabled", true)) {
                                player.spawnParticle(Particle.END_ROD, centerLoc, 15, 0.3f, 0.3f, 0.3f, 0.05);
                                player.spawnParticle(Particle.DUST, centerLoc, 10, 0.2f, 0.2f, 0.2f, glowDust);
                        }

                        BukkitTask[] taskHolder = new BukkitTask[1];
                        int[] tick = { 0 };

                        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                                tick[0]++;
                                if (tick[0] > durationTicks) {
                                        taskHolder[0].cancel();
                                        return;
                                }

                                if (pulseEnabled) {
                                        double pulse = Math.sin(tick[0] * 0.3) * ((sizeMax - sizeMin) / 2)
                                                        + (sizeMax + sizeMin) / 2;
                                        Particle.DustOptions pulseDust = new Particle.DustOptions(borderColor,
                                                        (float) pulse);

                                        for (int x = minX; x <= maxX; x += density) {
                                                player.spawnParticle(Particle.DUST,
                                                                new Location(world, x + 0.5, minY, minZ + 0.5), 1,
                                                                pulseDust);
                                                player.spawnParticle(Particle.DUST,
                                                                new Location(world, x + 0.5, minY, maxZ + 0.5), 1,
                                                                pulseDust);
                                                player.spawnParticle(Particle.GLOW,
                                                                new Location(world, x + 0.5, minY + 0.5, minZ + 0.5), 1,
                                                                0.1f, 0, 0.1f, 0.01f);
                                                player.spawnParticle(Particle.GLOW,
                                                                new Location(world, x + 0.5, minY + 0.5, maxZ + 0.5), 1,
                                                                0.1f, 0, 0.1f, 0.01f);
                                        }
                                        for (int z = minZ; z <= maxZ; z += density) {
                                                player.spawnParticle(Particle.DUST,
                                                                new Location(world, minX + 0.5, minY, z + 0.5), 1,
                                                                pulseDust);
                                                player.spawnParticle(Particle.DUST,
                                                                new Location(world, maxX + 0.5, minY, z + 0.5), 1,
                                                                pulseDust);
                                                player.spawnParticle(Particle.GLOW,
                                                                new Location(world, minX + 0.5, minY + 0.5, z + 0.5), 1,
                                                                0, 0.1f, 0.1f, 0.01f);
                                                player.spawnParticle(Particle.GLOW,
                                                                new Location(world, maxX + 0.5, minY + 0.5, z + 0.5), 1,
                                                                0, 0.1f, 0.1f, 0.01f);
                                        }

                                        for (int y = minY; y <= maxY; y += density) {
                                                player.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                                                new Location(world, minX + 0.5, y, minZ + 0.5), 1, 0.1f,
                                                                0.1f, 0.1f, 0.02f);
                                                player.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                                                new Location(world, maxX + 0.5, y, minZ + 0.5), 1, 0.1f,
                                                                0.1f, 0.1f, 0.02f);
                                                player.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                                                new Location(world, minX + 0.5, y, maxZ + 0.5), 1, 0.1f,
                                                                0.1f, 0.1f, 0.02f);
                                                player.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                                                new Location(world, maxX + 0.5, y, maxZ + 0.5), 1, 0.1f,
                                                                0.1f, 0.1f, 0.02f);
                                        }
                                }

                                double centerPulse = Math.sin(tick[0] * 0.5) * 0.5 + 1.0;
                                Particle.DustOptions centerGlow = new Particle.DustOptions(glowColor,
                                                (float) centerPulse);
                                player.spawnParticle(Particle.DUST, centerLoc, 3, 0.3f, 0.3f, 0.3f, centerGlow);

                                if (tick[0] % 8 == 0) {
                                        player.spawnParticle(Particle.GLOW, centerLoc, 5, 0.2f, 0.2f, 0.2f, 0.02);
                                        for (int x = minX; x <= maxX; x += density * 2) {
                                                player.spawnParticle(Particle.GLOW,
                                                                new Location(world, x + 0.5, minY + 1, minZ + 0.5), 1,
                                                                0.2f, 0, 0.2f, 0.02f);
                                                player.spawnParticle(Particle.GLOW,
                                                                new Location(world, x + 0.5, minY + 1, maxZ + 0.5), 1,
                                                                0.2f, 0, 0.2f, 0.02f);
                                        }
                                        for (int z = minZ; z <= maxZ; z += density * 2) {
                                                player.spawnParticle(Particle.GLOW,
                                                                new Location(world, minX + 0.5, minY + 1, z + 0.5), 1,
                                                                0, 0.2f, 0.2f, 0.02f);
                                                player.spawnParticle(Particle.GLOW,
                                                                new Location(world, maxX + 0.5, minY + 1, z + 0.5), 1,
                                                                0, 0.2f, 0.2f, 0.02f);
                                        }
                                }

                                if (tick[0] % 10 == 0) {
                                        player.spawnParticle(Particle.DUST, corner1.clone().add(0, 1, 0), 10, 0.3f,
                                                        0.3f, 0.3f, cornerDust);
                                        player.spawnParticle(Particle.DUST, corner2.clone().add(0, 1, 0), 10, 0.3f,
                                                        0.3f, 0.3f, cornerDust);
                                        player.spawnParticle(Particle.DUST, corner3.clone().add(0, 1, 0), 10, 0.3f,
                                                        0.3f, 0.3f, cornerDust);
                                        player.spawnParticle(Particle.DUST, corner4.clone().add(0, 1, 0), 10, 0.3f,
                                                        0.3f, 0.3f, cornerDust);
                                }

                                if (tick[0] % 20 == 0) {
                                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, corner1.clone().add(0, 2, 0), 10,
                                                        0.3f, 0.5f, 0.3f, 0.05);
                                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, corner2.clone().add(0, 2, 0), 10,
                                                        0.3f, 0.5f, 0.3f, 0.05);
                                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, corner3.clone().add(0, 2, 0), 10,
                                                        0.3f, 0.5f, 0.3f, 0.05);
                                        player.spawnParticle(Particle.SOUL_FIRE_FLAME, corner4.clone().add(0, 2, 0), 10,
                                                        0.3f, 0.5f, 0.3f, 0.05);
                                }

                                if (tick[0] % 30 == 0) {
                                        for (int y = minY; y <= maxY; y += density * 4) {
                                                player.spawnParticle(Particle.END_ROD,
                                                                corner1.clone().add(0, y - minY, 0), 3, 0.2f, 0.2f,
                                                                0.2f, 0.05);
                                                player.spawnParticle(Particle.END_ROD,
                                                                corner2.clone().add(0, y - minY, 0), 3, 0.2f, 0.2f,
                                                                0.2f, 0.05);
                                                player.spawnParticle(Particle.END_ROD,
                                                                corner3.clone().add(0, y - minY, 0), 3, 0.2f, 0.2f,
                                                                0.2f, 0.05);
                                                player.spawnParticle(Particle.END_ROD,
                                                                corner4.clone().add(0, y - minY, 0), 3, 0.2f, 0.2f,
                                                                0.2f, 0.05);
                                        }
                                }

                                if (tick[0] % 15 == 0) {
                                        int sweepOffset = (tick[0] / 15) % 4;
                                        switch (sweepOffset) {
                                                case 0 -> {
                                                        for (int x = minX; x <= maxX; x += density) {
                                                                player.spawnParticle(Particle.WITCH,
                                                                                new Location(world, x + 0.5, minY + 1,
                                                                                                minZ + 0.5),
                                                                                2, 0.1f, 0.1f, 0.1f, 0.05);
                                                        }
                                                }
                                                case 1 -> {
                                                        for (int z = minZ; z <= maxZ; z += density) {
                                                                player.spawnParticle(Particle.WITCH,
                                                                                new Location(world, maxX + 0.5,
                                                                                                minY + 1, z + 0.5),
                                                                                2, 0.1f, 0.1f, 0.1f, 0.05);
                                                        }
                                                }
                                                case 2 -> {
                                                        for (int x = maxX; x >= minX; x -= density) {
                                                                player.spawnParticle(Particle.WITCH,
                                                                                new Location(world, x + 0.5, minY + 1,
                                                                                                maxZ + 0.5),
                                                                                2, 0.1f, 0.1f, 0.1f, 0.05);
                                                        }
                                                }
                                                case 3 -> {
                                                        for (int z = maxZ; z >= minZ; z -= density) {
                                                                player.spawnParticle(Particle.WITCH,
                                                                                new Location(world, minX + 0.5,
                                                                                                minY + 1, z + 0.5),
                                                                                2, 0.1f, 0.1f, 0.1f, 0.05);
                                                        }
                                                }
                                        }
                                }
                        }, 5L, pulseInterval);
                });
        }

        private static Color parseColor(String colorStr) {
                String[] parts = colorStr.split(",");
                if (parts.length == 3) {
                        try {
                                return Color.fromRGB(
                                                Integer.parseInt(parts[0].trim()),
                                                Integer.parseInt(parts[1].trim()),
                                                Integer.parseInt(parts[2].trim()));
                        } catch (NumberFormatException e) {
                                return Color.fromRGB(0, 200, 255);
                        }
                }
                return Color.fromRGB(0, 200, 255);
        }
}