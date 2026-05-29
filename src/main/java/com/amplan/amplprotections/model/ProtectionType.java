package com.amplan.amplprotections.model;

import org.bukkit.Material;

public class ProtectionType {
    private final String id;
    private final Material material;
    private final int customModelData;
    private final String displayName;
    private final int radius;
    private final String skullValue;
    private final boolean hideBlock;
    private final boolean glowOnView;
    private final boolean enableMerge;
    private final int maxMergeRadius;

    public ProtectionType(String id, Material material, int customModelData, String displayName, int radius) {
        this(id, material, customModelData, displayName, radius, "", false, true, false, 0);
    }

    public ProtectionType(String id, Material material, int customModelData, String displayName, int radius, String skullValue, boolean hideBlock, boolean glowOnView) {
        this(id, material, customModelData, displayName, radius, skullValue, hideBlock, glowOnView, false, 0);
    }

    public ProtectionType(String id, Material material, int customModelData, String displayName, int radius, String skullValue, boolean hideBlock, boolean glowOnView, boolean enableMerge, int maxMergeRadius) {
        this.id = id;
        this.material = material;
        this.customModelData = customModelData;
        this.displayName = displayName;
        this.radius = radius;
        this.skullValue = skullValue;
        this.hideBlock = hideBlock;
        this.glowOnView = glowOnView;
        this.enableMerge = enableMerge;
        this.maxMergeRadius = maxMergeRadius;
    }

    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public String getDisplayName() { return displayName; }
    public int getRadius() { return radius; }
    public String getSkullValue() { return skullValue; }
    public boolean isHideBlock() { return hideBlock; }
    public boolean isGlowOnView() { return glowOnView; }
    public boolean isEnableMerge() { return enableMerge; }
    public int getMaxMergeRadius() { return maxMergeRadius; }
    public boolean canMerge() { return enableMerge; }
}
