package com.amplan.amplprotections.model;

public enum FlagPermissionLevel {

    NONE("Nadie", "<red>NADIE", "<red>Ni siquiera el dueño", 0),
    OWNER("Solo dueño", "<gold>SOLO DUEÑO", "<gold>Solo el dueño puede", 1),
    MEMBERS("Miembros", "<green>MIEMBROS", "<green>Solo miembros", 2),
    ADMINS("Admins+", "<yellow>ADMINS+", "<yellow>Solo admins y superiores", 3),
    EVERYONE("Todos", "<aqua>TODOS", "<aqua>Todos los jugadores", 4);

    private final String label;
    private final String displayName;
    private final String description;
    private final int value;

    FlagPermissionLevel(String label, String displayName, String description, int value) {
        this.label = label;
        this.displayName = displayName;
        this.description = description;
        this.value = value;
    }

    public String getLabel() { return label; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getValue() { return value; }

    public FlagPermissionLevel next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public FlagPermissionLevel nextSimple() {
        return this == NONE ? EVERYONE : NONE;
    }

    public FlagPermissionLevel previous() {
        FlagPermissionLevel[] vals = values();
        return vals[(ordinal() - 1 + vals.length) % vals.length];
    }

    public FlagPermissionLevel previousSimple() {
        return this == EVERYONE ? NONE : EVERYONE;
    }

    public static FlagPermissionLevel fromBoolean(boolean value) {
        return value ? EVERYONE : NONE;
    }

    public static FlagPermissionLevel fromInt(int value) {
        for (FlagPermissionLevel level : values()) {
            if (level.value == value) return level;
        }
        if (value == 1) return MEMBERS;
        if (value == 2) return ADMINS;
        if (value == 3) return EVERYONE;
        return NONE;
    }

    public static FlagPermissionLevel fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            try {
                return fromInt(Integer.parseInt(value));
            } catch (NumberFormatException ex) {
                return NONE;
            }
        }
    }

    public static boolean isEnvironmental(String flag) {
        return switch (flag.toLowerCase()) {
            case "mob-spawn", "leaf-decay", "fire-spread", "fire-damage", "fire-ignite",
                 "lava-flow", "water-flow", "ice-melt", "snow-melt", "crop-trample", "soil-dry" -> true;
            default -> false;
        };
    }
}
