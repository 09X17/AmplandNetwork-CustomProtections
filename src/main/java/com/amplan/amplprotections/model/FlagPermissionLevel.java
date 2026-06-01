package com.amplan.amplprotections.model;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.language.LanguageManager;

public enum FlagPermissionLevel {

    NONE(0),
    OWNER(1),
    MEMBERS(2),
    ADMINS(3),
    EVERYONE(4);

    private final int value;

    FlagPermissionLevel(int value) {
        this.value = value;
    }

    public String getLabel() {
        return getLangString("display");
    }

    public String getDisplayName() {
        return getLangString("display");
    }

    public String getDescription() {
        return getLangString("description");
    }

    public String getColor() {
        return getLangString("color");
    }

    public String getSetMessage() {
        return getLangString("set-message");
    }

    private String getLangString(String suffix) {
        LanguageManager langManager = AmplProtections.getInstance().getLanguageManager();
        if (langManager == null) {
            return name();
        }
        String key = "flag-levels." + name().toLowerCase() + "." + suffix;
        String val = langManager.getString(key);
        return val != null ? val : name();
    }

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
