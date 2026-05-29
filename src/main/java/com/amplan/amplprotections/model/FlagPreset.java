package com.amplan.amplprotections.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlagPreset {

    private int id;
    private String name;
    private final UUID ownerUuid;
    private final boolean isGlobal;
    private String description;
    private final Map<String, FlagPermissionLevel> flags = new ConcurrentHashMap<>();

    public FlagPreset(String name, UUID ownerUuid, boolean isGlobal) {
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.isGlobal = isGlobal;
        this.description = "";
    }

    public FlagPreset(int id, String name, UUID ownerUuid, boolean isGlobal, String description) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.isGlobal = isGlobal;
        this.description = description != null ? description : "";
    }

    public void setFlag(String key, FlagPermissionLevel level) {
        flags.put(key, level);
    }

    public FlagPermissionLevel getFlag(String key) {
        return flags.getOrDefault(key, FlagPermissionLevel.NONE);
    }

    public Map<String, FlagPermissionLevel> getFlags() {
        return flags;
    }

    public void applyToRegion(ProtectionRegion region) {
        for (Map.Entry<String, FlagPermissionLevel> entry : flags.entrySet()) {
            region.setFlagLevel(entry.getKey(), entry.getValue());
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
