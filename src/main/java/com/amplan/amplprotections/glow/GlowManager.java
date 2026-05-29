package com.amplan.amplprotections.glow;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.model.ProtectionType;
import com.amplan.amplprotections.utils.SkullUtils;

import net.kyori.adventure.text.format.NamedTextColor;

public class GlowManager {

    private final AmplProtections plugin;
    private final Map<Integer, UUID> regionGlowEntities = new HashMap<>();
    private Team glowTeam;

    public GlowManager(AmplProtections plugin) {
        this.plugin = plugin;
        setupGlowTeam();
    }

    private void setupGlowTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "ap_glow";
        Team existing = scoreboard.getTeam(teamName);
        if (existing != null) {
            existing.unregister();
        }
        glowTeam = scoreboard.registerNewTeam(teamName);
        String color = plugin.getConfig().getString("glow.color");
        if (color == null) {
            color = "gold";
        }
        try {
            NamedTextColor namedColor = NamedTextColor.NAMES.value(color.toLowerCase(Locale.ROOT));
            glowTeam.color(namedColor != null
                    ? namedColor
                    : NamedTextColor.GOLD);
        } catch (Exception e) {
            glowTeam.color(NamedTextColor.GOLD);
        }
        glowTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        glowTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    public void spawnGlow(ProtectionRegion region) {
        if (!plugin.getConfig().getBoolean("glow.enabled", true))
            return;

        removeGlow(region);

        org.bukkit.World world = Bukkit.getWorld(region.getWorldName());
        if (world == null)
            return;

        Location loc = new Location(world, region.getBlockX() + 0.5, region.getBlockY(), region.getBlockZ() + 0.5);

        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setMarker(true);
        stand.setBasePlate(false);

        PotionEffectType glowingType = PotionEffectType.GLOWING;
        if (glowingType != null) {
            stand.addPotionEffect(
                    new PotionEffect(glowingType, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        }

        if (glowTeam != null) {
            glowTeam.addEntry(stand.getUniqueId().toString());
        }

        ProtectionManager manager = plugin.getProtectionManager();
        ProtectionType type = manager.getProtectionTypes().get(region.getTypeId());
        if (type != null) {
            ItemStack helmet;
            if (type.getMaterial() == Material.PLAYER_HEAD && type.getSkullValue() != null
                    && !type.getSkullValue().isEmpty()) {
                helmet = SkullUtils.createSkullWithTexture(type.getSkullValue());
            } else {
                helmet = new ItemStack(type.getMaterial());
            }
            stand.getEquipment().setHelmet(helmet);
        } else {
            org.bukkit.block.Block block = loc.getBlock();
            if (block.getType() != Material.AIR) {
                EntityEquipment equipment = stand.getEquipment();
                if (equipment != null) {
                    equipment.setHelmet(new ItemStack(block.getType()));
                }
            }
        }

        regionGlowEntities.put(region.getDatabaseId(), stand.getUniqueId());
    }

    public void spawnGlowTemp(ProtectionRegion region, long durationTicks) {
        spawnGlow(region);
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeGlow(region), durationTicks);
    }

    public void removeGlow(ProtectionRegion region) {
        UUID uuid = regionGlowEntities.remove(region.getDatabaseId());
        if (uuid != null) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
                if (glowTeam != null) {
                    glowTeam.removeEntry(entity.getUniqueId().toString());
                }
            }
        }
    }

    public void removeAll() {
        for (UUID uuid : regionGlowEntities.values()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null)
                entity.remove();
        }
        regionGlowEntities.clear();
    }
}
