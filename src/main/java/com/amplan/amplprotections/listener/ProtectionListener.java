package com.amplan.amplprotections.listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.menu.MainProtectionMenu;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.MessageUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class ProtectionListener implements Listener {

    private final AmplProtections plugin;
    private final ProtectionManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, ProtectionRegion> lastRegion = new HashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> lastAccessDenied = new ConcurrentHashMap<>();
    private Set<String> silentFlags;
    private long accessDeniedCooldown;

    public ProtectionListener(AmplProtections plugin) {
        this.plugin = plugin;
        this.manager = plugin.getProtectionManager();
        loadConfigCache();
    }

    private void loadConfigCache() {
        silentFlags = new HashSet<>(plugin.getConfig().getStringList("flags.silent-flags"));
        accessDeniedCooldown = plugin.getConfig().getLong("flags.access-denied-cooldown-ms", 3000);
    }

    public void reload() {
        loadConfigCache();
    }

    private boolean canAct(Location loc, String flag, Player player) {
        ProtectionRegion region = manager.getRegionAt(loc);
        if (region == null)
            return true;
        if (player != null && player.hasPermission("amplprotections.admin.bypass"))
            return true;
        if (player != null && region.canPlayerAct(flag, player.getUniqueId()))
            return true;
        if (player != null)
            sendAccessDenied(player, flag);
        return false;
    }

    private boolean isSilentFlag(String flag) {
        return silentFlags.contains(flag);
    }

    private long getAccessDeniedCooldown() {
        return accessDeniedCooldown;
    }

    private void sendAccessDenied(Player player, String flag) {
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
            msg = MessageUtils.lang("access-denied.default");
        }
        player.sendMessage(mm.deserialize(msg));
    }

    private void sendAccessDeniedCooldown(Player player, String key, String defaultMessage) {
        if (isSilentFlag(key))
            return;

        long now = System.currentTimeMillis();
        long cooldown = getAccessDeniedCooldown();
        ConcurrentHashMap<String, Long> playerTimers = lastAccessDenied.computeIfAbsent(player.getUniqueId(),
                k -> new ConcurrentHashMap<>());
        Long lastTime = playerTimers.get(key);
        if (lastTime != null && (now - lastTime) < cooldown)
            return;
        playerTimers.put(key, now);
        player.sendMessage(mm.deserialize(defaultMessage));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProtectionBlockInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;

        Location blockLoc = clickedBlock.getLocation();
        ProtectionRegion region = manager.getRegionAt(blockLoc);

        if (region == null)
            return;

        if (blockLoc.getBlockX() == region.getBlockX() &&
                blockLoc.getBlockY() == region.getBlockY() &&
                blockLoc.getBlockZ() == region.getBlockZ()) {

            event.setCancelled(true);

            Player player = event.getPlayer();
            if (region.getOwnerUniqueId().equals(player.getUniqueId())
                    || player.hasPermission("amplprotections.admin.bypass")) {
                plugin.getMenuManager().openMenu(player, new MainProtectionMenu(plugin, region));
            } else {
                sendAccessDeniedCooldown(player, "protection-block",
                        MessageUtils.lang("access-denied.protection-block"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        ProtectionRegion region = manager.getRegionAt(loc);
        if (region != null) {
            if (loc.getBlockX() == region.getBlockX() && loc.getBlockY() == region.getBlockY()
                    && loc.getBlockZ() == region.getBlockZ())
                return;
            if (!canAct(loc, "block-break", event.getPlayer()))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Location loc = event.getBlockPlaced().getLocation();
        ProtectionRegion region = manager.getRegionAt(loc);
        if (region != null) {
            if (loc.getBlockX() == region.getBlockX() && loc.getBlockY() == region.getBlockY()
                    && loc.getBlockZ() == region.getBlockZ())
                return;
            if (!canAct(loc, "block-place", event.getPlayer()))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;

        Location blockLoc = clickedBlock.getLocation();
        ProtectionRegion region = manager.getRegionAt(blockLoc);
        if (region == null)
            return;

        if (blockLoc.getBlockX() == region.getBlockX() && blockLoc.getBlockY() == region.getBlockY()
                && blockLoc.getBlockZ() == region.getBlockZ())
            return;

        Player player = event.getPlayer();
        if (player.hasPermission("amplprotections.admin.bypass"))
            return;

        String flag = switch (clickedBlock.getType()) {
            case OAK_DOOR, IRON_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR, ACACIA_DOOR, DARK_OAK_DOOR, MANGROVE_DOOR,
                    CHERRY_DOOR,
                    OAK_TRAPDOOR, IRON_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, JUNGLE_TRAPDOOR, ACACIA_TRAPDOOR,
                    DARK_OAK_TRAPDOOR ->
                "use-doors";
            case CHEST, BARREL, HOPPER, TRAPPED_CHEST, SHULKER_BOX -> "use-chests";
            case LEVER, STONE_BUTTON, OAK_BUTTON, SPRUCE_BUTTON -> "use-switches";
            case CRAFTING_TABLE, FURNACE, ANVIL, ENCHANTING_TABLE, BREWING_STAND, BEACON -> "use-crafting";
            default -> null;
        };

        if (flag != null && !region.canPlayerAct(flag, player.getUniqueId())) {
            event.setCancelled(true);
            sendAccessDenied(player, flag);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractPhysical(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL)
            return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;
        ProtectionRegion region = manager.getRegionAt(clickedBlock.getLocation());
        if (region == null)
            return;
        Player player = event.getPlayer();
        if (region.isMember(player.getUniqueId()) || player.hasPermission("amplprotections.admin.bypass"))
            return;
        if (clickedBlock.getType() == Material.FARMLAND) {
            if (!region.isBooleanFlagEnabled("crop-trample")) {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == org.bukkit.entity.EntityType.ARMOR_STAND) {
            if (!canAct(event.getRightClicked().getLocation(), "armor-stand-edit", event.getPlayer()))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnimalInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.Animals))
            return;
        if (!canAct(event.getRightClicked().getLocation(), "use-animals", event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        ProtectionRegion region = manager.getRegionAt(event.getLocation());
        if (region != null && !region.isMobSpawnEnabled())
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ())
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ProtectionRegion to = manager.getRegionAt(event.getTo());
        ProtectionRegion last = lastRegion.get(uuid);

        if (to != null && !to.equals(last)) {
            if (!player.hasPermission("amplprotections.admin.bypass") && !to.canPlayerAct("entry", uuid)) {
                event.setCancelled(true);
                sendAccessDenied(player, "entry");
                return;
            }
            String ownerName = manager.getCachedOwnerName(to.getOwnerUniqueId());
            String enterMsg = MessageUtils.lang("protection.enter").replace("%owner%", ownerName);
            player.sendActionBar(mm.deserialize(enterMsg));
            lastRegion.put(uuid, to);
        } else if (to == null && last != null) {
            String ownerName = manager.getCachedOwnerName(last.getOwnerUniqueId());
            String leaveMsg = MessageUtils.lang("protection.leave").replace("%owner%", ownerName);
            player.sendActionBar(mm.deserialize(leaveMsg));
            lastRegion.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        ProtectionRegion region = manager.getRegionAt(event.getBlock().getLocation());
        if (region != null && !region.isBooleanFlagEnabled("fire-spread"))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        ProtectionRegion region = manager.getRegionAt(event.getBlock().getLocation());
        if (region != null && !region.isBooleanFlagEnabled("fire-ignite"))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        Location toLoc = event.getToBlock().getLocation();
        ProtectionRegion region = manager.getRegionAt(toLoc);
        if (region == null)
            return;
        String flag = event.getBlock().isLiquid()
                ? (event.getBlock().getType().name().contains("LAVA") ? "lava-flow" : "water-flow")
                : null;
        if (flag != null && !region.isBooleanFlagEnabled(flag))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!canAct(event.getPlayer().getLocation(), "item-drop", event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPickupItem(org.bukkit.event.player.PlayerAttemptPickupItemEvent event) {
        if (!canAct(event.getPlayer().getLocation(), "item-pickup", event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!canAct(event.getFrom(), "use-portals", event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker))
            return;
        if (event.getEntity() instanceof Player)
            return;
        if (!canAct(event.getEntity().getLocation(), "mob-damage", attacker))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractItemFrame(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame))
            return;
        if (!canAct(event.getRightClicked().getLocation(), "frame-rotate", event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehiclePlace(org.bukkit.event.vehicle.VehicleCreateEvent event) {
        List<org.bukkit.entity.Entity> passengers = event.getVehicle().getPassengers();
        Player player = null;
        if (!passengers.isEmpty() && passengers.get(0) instanceof Player p) {
            player = p;
        }
        if (!canAct(event.getVehicle().getLocation(), "vehicle-place", player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player))
            return;
        ProtectionRegion region = manager.getRegionAt(player.getLocation());
        if (region == null)
            return;
        String projectileName = event.getEntity().getType().name();
        if (projectileName.contains("POTION") && !region.canPlayerAct("potion-splash", player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == org.bukkit.entity.EntityType.VILLAGER ||
                event.getRightClicked().getType() == org.bukkit.entity.EntityType.WANDERING_TRADER) {
            if (!canAct(event.getRightClicked().getLocation(), "use-villager", event.getPlayer())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType().name().contains("BED")) {
            if (!canAct(
                    block.getLocation(),
                    "use-beds",
                    event.getPlayer())) {

                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFrameBreak(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame) {
            ProtectionRegion region = manager.getRegionAt(event.getEntity().getLocation());
            if (region != null && !region.isFlagEnabled("frame-break")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPaintingBreak(HangingBreakEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Painting) {
            ProtectionRegion region = manager.getRegionAt(event.getEntity().getLocation());
            if (region != null && !region.isFlagEnabled("painting-break")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleBreak(VehicleDestroyEvent event) {
        if (!(event.getAttacker() instanceof Player player))
            return;
        if (!canAct(event.getVehicle().getLocation(), "vehicle-break", player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropTrample(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() == Material.FARMLAND) {
            ProtectionRegion region = manager.getRegionAt(block.getLocation());
            if (region != null && !region.isBooleanFlagEnabled("crop-trample")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSoilDry(BlockFadeEvent event) {
        if (event.getBlock().getType() == Material.FARMLAND) {
            ProtectionRegion region = manager.getRegionAt(event.getBlock().getLocation());
            if (region != null && !region.isBooleanFlagEnabled("soil-dry")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            ProtectionRegion region = manager.getRegionAt(event.getEntity().getLocation());
            if (region != null && !region.isBooleanFlagEnabled("fire-damage")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIceMelt(BlockFadeEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.ICE || type == Material.PACKED_ICE || type == Material.BLUE_ICE) {
            ProtectionRegion region = manager.getRegionAt(event.getBlock().getLocation());
            if (region != null && !region.isBooleanFlagEnabled("ice-melt")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSnowMelt(BlockFadeEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.SNOW || type == Material.SNOW_BLOCK) {
            ProtectionRegion region = manager.getRegionAt(event.getBlock().getLocation());
            if (region != null && !region.isBooleanFlagEnabled("snow-melt")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeafDecay(org.bukkit.event.block.LeavesDecayEvent event) {
        ProtectionRegion region = manager.getRegionAt(event.getBlock().getLocation());
        if (region != null && !region.isBooleanFlagEnabled("leaf-decay")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastAccessDenied.remove(event.getPlayer().getUniqueId());
        lastRegion.remove(event.getPlayer().getUniqueId());
    }
}
