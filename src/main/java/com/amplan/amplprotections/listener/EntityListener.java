package com.amplan.amplprotections.listener;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.MessageUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class EntityListener implements Listener {

    private final AmplProtections plugin;
    private final ProtectionManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> lastAccessDenied = new ConcurrentHashMap<>();

    public EntityListener(AmplProtections plugin) {
        this.plugin = plugin;
        this.manager = plugin.getProtectionManager();
    }

    private boolean isSilentFlag(String flag) {
        return plugin.getConfig().getStringList("flags.silent-flags").contains(flag);
    }

    private long getAccessDeniedCooldown() {
        return plugin.getConfig().getLong("flags.access-denied-cooldown-ms", 3000);
    }

    private void sendAccessDenied(Player player, String flag, String defaultMessage) {
        if (isSilentFlag(flag))
            return;

        long now = System.currentTimeMillis();
        long cooldown = getAccessDeniedCooldown();
        ConcurrentHashMap<String, Long> playerTimers = lastAccessDenied.computeIfAbsent(player.getUniqueId(),
                k -> new ConcurrentHashMap<>());
        Long lastTime = playerTimers.get(flag);
        if (lastTime != null && (now - lastTime) < cooldown)
            return;
        playerTimers.put(flag, now);

        String msg = MessageUtils.lang("access-denied." + flag);
        if (msg == null || msg.isEmpty()) {
            msg = defaultMessage;
        }
        player.sendMessage(mm.deserialize(msg));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player damaged))
            return;
        Player attacker = switch (event.getDamager()) {
            case Player playerAttacker -> playerAttacker;
            case Projectile projectile when projectile.getShooter() instanceof Player shooterAttacker ->
                shooterAttacker;
            default -> null;
        };

        if (attacker == null)
            return;
        ProtectionRegion region = manager.getRegionAt(damaged.getLocation());
        if (region == null) {
            region = manager.getRegionAt(attacker.getLocation());
        }
        if (region != null && !region.canPlayerAct("pvp", attacker.getUniqueId())) {
            if (!attacker.hasPermission("amplprotections.admin.bypass")) {
                sendAccessDenied(attacker, "pvp",
                        "<red> El PvP se encuentra deshabilitado en esta zona de protección.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            Location blockLoc = block.getLocation();
            ProtectionRegion region = manager.getRegionAt(blockLoc);
            if (region != null) {
                if (region.getBlockX() == blockLoc.getBlockX()
                        && region.getBlockY() == blockLoc.getBlockY()
                        && region.getBlockZ() == blockLoc.getBlockZ()) {
                    blockIterator.remove();
                    continue;
                }
                if (!region.isBooleanFlagEnabled("tnt") || !region.isBooleanFlagEnabled("explosions")) {
                    blockIterator.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> blockIterator = event.blockList().iterator();

        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            Location blockLoc = block.getLocation();
            ProtectionRegion region = manager.getRegionAt(blockLoc);

            if (region != null) {
                if (region.getBlockX() == blockLoc.getBlockX() &&
                        region.getBlockY() == blockLoc.getBlockY() &&
                        region.getBlockZ() == blockLoc.getBlockZ()) {
                    blockIterator.remove();
                    continue;
                }

                if (!region.isBooleanFlagEnabled("tnt") || !region.isBooleanFlagEnabled("explosions")) {
                    blockIterator.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Monster))
            return;

        Location spawnLoc = event.getLocation();
        ProtectionRegion region = manager.getRegionAt(spawnLoc);

        if (region != null && !region.isMobSpawnEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageGeneral(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player)
            return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p)
            attacker = p;
        else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p)
            attacker = p;

        if (attacker == null)
            return;

        ProtectionRegion region = manager.getRegionAt(event.getEntity().getLocation());
        if (region != null && !region.canPlayerAct("entity-damage", attacker.getUniqueId())) {
            if (!attacker.hasPermission("amplprotections.admin.bypass")) {
                sendAccessDenied(attacker, "entity-damage", "<red>❌ No puedes dañar entidades aquí.");
                event.setCancelled(true);
            }
        }
    }
}
