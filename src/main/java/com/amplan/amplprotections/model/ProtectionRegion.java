package com.amplan.amplprotections.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

public class ProtectionRegion {

    private static final AtomicInteger landIdCounter = new AtomicInteger(0);

    private int databaseId;
    private String landId;
    private final UUID ownerUniqueId;
    private final String worldName;
    private String customName;
    private String customLore;
    private final int blockX, blockY, blockZ;
    private final int minX, maxX, minZ, maxZ, minY, maxY;
    private final String typeId;
    private boolean hologramEnabled;

    private final ConcurrentHashMap<String, FlagPermissionLevel> flags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PlayerRank> members = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> memberJoinDates = new ConcurrentHashMap<>();

    public ProtectionRegion(UUID ownerUniqueId, Location centerBlock, ProtectionType type) {
        this.databaseId = -1;
        this.landId = generateLandId();
        this.ownerUniqueId = ownerUniqueId;
        this.worldName = centerBlock.getWorld().getName();
        this.customName = type.getDisplayName();
        this.customLore = "";
        this.blockX = centerBlock.getBlockX();
        this.blockY = centerBlock.getBlockY();
        this.blockZ = centerBlock.getBlockZ();
        
        int radius = type.getRadius();
        this.minX = blockX - radius;
        this.maxX = blockX + radius;
        this.minZ = blockZ - radius;
        this.maxZ = blockZ + radius;

        World world = centerBlock.getWorld();
        this.minY = world.getMinHeight();
        this.maxY = world.getMaxHeight();
        this.typeId = type.getId();

        setDefaultFlags();
    }

    public ProtectionRegion(int databaseId, UUID ownerUniqueId, String worldName, String customName,
                            int blockX, int blockY, int blockZ,
                            int minX, int maxX, int minZ, int maxZ, int minY, int maxY,
                            String typeId) {
        this(databaseId, ownerUniqueId, worldName, customName, "", blockX, blockY, blockZ, minX, maxX, minZ, maxZ, minY, maxY, typeId, null);
    }

    public ProtectionRegion(int databaseId, UUID ownerUniqueId, String worldName, String customName, String customLore,
                            int blockX, int blockY, int blockZ,
                            int minX, int maxX, int minZ, int maxZ, int minY, int maxY,
                            String typeId) {
        this(databaseId, ownerUniqueId, worldName, customName, customLore, blockX, blockY, blockZ, minX, maxX, minZ, maxZ, minY, maxY, typeId, null);
    }

    public ProtectionRegion(int databaseId, UUID ownerUniqueId, String worldName, String customName, String customLore,
                            int blockX, int blockY, int blockZ,
                            int minX, int maxX, int minZ, int maxZ, int minY, int maxY,
                            String typeId, String landId) {
        this.databaseId = databaseId;
        this.landId = landId != null ? landId : generateLandId();
        this.ownerUniqueId = ownerUniqueId;
        this.worldName = worldName;
        this.customName = customName;
        this.customLore = customLore != null ? customLore : "";
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.minY = minY;
        this.maxY = maxY;
        this.typeId = typeId;
    }

    private static String generateLandId() {
        int num = landIdCounter.incrementAndGet();
        return "LAND" + num;
    }

    public static void resetCounter(int startFrom) {
        landIdCounter.set(startFrom);
    }

    private void setDefaultFlags() {
        String[] flagsOwner = {
            "pvp", "mob-damage", "entity-damage", "tnt", "explosions", "potion-splash",
            "block-break", "block-place",
            "use-doors", "use-switches", "use-chests", "use-crafting", "use-animals",
            "use-portals", "use-beds", "use-villager",
            "frame-rotate", "frame-break", "armor-stand-edit", "painting-break",
            "vehicle-place", "vehicle-break", "item-drop", "item-pickup"
        };
        String[] flagsEveryone = {
            "mob-spawn", "leaf-decay", "fire-spread", "fire-damage", "fire-ignite",
            "lava-flow", "water-flow", "ice-melt", "snow-melt", "crop-trample", "soil-dry"
        };
        for (String f : flagsOwner) flags.put(f, FlagPermissionLevel.OWNER);
        for (String f : flagsEveryone) flags.put(f, FlagPermissionLevel.EVERYONE);
    }

    public boolean intersects(ProtectionRegion other) {
        if (!this.worldName.equals(other.worldName)) return false;
        return this.minX <= other.maxX && this.maxX >= other.minX &&
               this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
    }

    public boolean isPvpEnabled() { return getFlagLevel("pvp") != FlagPermissionLevel.NONE; }
    public boolean isTntEnabled() { return getFlagLevel("tnt") != FlagPermissionLevel.NONE; }
    public boolean isMobSpawnEnabled() { return getFlagLevel("mob-spawn") == FlagPermissionLevel.EVERYONE; }
    public int getRadius() { return (maxX - minX) / 2; }

    public String getOwnerName() {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUniqueId);
        return owner.getName() != null ? owner.getName() : ownerUniqueId.toString().substring(0, 8);
    }

    public PlayerRank getRank(UUID uuid) {
        if (uuid.equals(ownerUniqueId)) return PlayerRank.OWNER;
        return members.get(uuid);
    }

    public void setFlag(String flag, boolean value) {
        flags.put(flag.toLowerCase(), FlagPermissionLevel.fromBoolean(value));
    }

    public void setFlagLevel(String flag, FlagPermissionLevel level) {
        flags.put(flag.toLowerCase(), level);
    }

    public FlagPermissionLevel getFlagLevel(String flag) {
        return flags.getOrDefault(flag.toLowerCase(), FlagPermissionLevel.NONE);
    }

    public boolean isFlagEnabled(String flag) {
        return getFlagLevel(flag) == FlagPermissionLevel.EVERYONE;
    }

    @Deprecated
    public boolean isAllowed(String flag) {
        return isFlagEnabled(flag);
    }

    public boolean canPerformAction(String flag, PlayerRank playerRank) {
        if (playerRank == null) playerRank = PlayerRank.MEMBER;
        FlagPermissionLevel level = getFlagLevel(flag);
        return switch (level) {
            case NONE -> false;
            case OWNER -> playerRank == PlayerRank.OWNER;
            case MEMBERS -> playerRank != PlayerRank.MEMBER;
            case ADMINS -> playerRank == PlayerRank.ADMIN || playerRank == PlayerRank.SECONDARY_OWNER || playerRank == PlayerRank.OWNER;
            case EVERYONE -> true;
        };
    }

    public boolean canPlayerAct(String flag, UUID playerUuid) {
        FlagPermissionLevel level = getFlagLevel(flag);
        if (level == FlagPermissionLevel.NONE) return false;
        if (ownerUniqueId.equals(playerUuid)) return true;
        PlayerRank rank = getRank(playerUuid);
        if (rank == null) return false;
        return canPerformAction(flag, rank);
    }
    
    public void addMember(UUID uuid, PlayerRank rank) {
        members.put(uuid, rank);
        memberJoinDates.putIfAbsent(uuid, System.currentTimeMillis());
    }
    public void removeMember(UUID uuid) {
        members.remove(uuid);
        memberJoinDates.remove(uuid);
    }
    public boolean isMember(UUID uuid) { return uuid.equals(ownerUniqueId) || members.containsKey(uuid); }
    public Long getMemberJoinDate(UUID uuid) { return memberJoinDates.get(uuid); }
    public Map<UUID, Long> getMemberJoinDates() { return memberJoinDates; }
    public void setMemberJoinDate(UUID uuid, long timestamp) { memberJoinDates.put(uuid, timestamp); }

    public int getDatabaseId() { return databaseId; }
    public void setDatabaseId(int id) { this.databaseId = id; }
    public String getLandId() { return landId; }
    public void setLandId(String landId) { this.landId = landId; }
    public UUID getOwnerUniqueId() { return ownerUniqueId; }
    public String getWorldName() { return worldName; }
    public String getCustomName() { return customName; }
    public void setCustomName(String name) { this.customName = name; }
    public String getCustomLore() { return customLore; }
    public void setCustomLore(String lore) { this.customLore = lore; }
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public String getTypeId() { return typeId; }
    public Map<String, FlagPermissionLevel> getFlags() { return flags; }
    public Map<UUID, PlayerRank> getMembers() { return members; }
    public boolean isHologramEnabled() { return hologramEnabled; }
    public void setHologramEnabled(boolean enabled) { this.hologramEnabled = enabled; }
}
