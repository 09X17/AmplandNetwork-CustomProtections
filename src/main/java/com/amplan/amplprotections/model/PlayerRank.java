package com.amplan.amplprotections.model;

public enum PlayerRank {
    MEMBER(1, "Miembro"),
    ADMIN(2, "Administrador"),
    SECONDARY_OWNER(3, "Dueno Secundario"),
    OWNER(4, "Dueno");

    private final int weight;
    private final String displayName;

    PlayerRank(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Verifica si este rango puede realizar acciones de administracion (como ascender o cambiar flags).
     */
    public boolean canManage() {
        return this == ADMIN || this == SECONDARY_OWNER || this == OWNER;
    }
}
