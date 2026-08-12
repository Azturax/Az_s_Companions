package com.azscompanions.entity;

import net.minecraft.util.StringRepresentable;

public enum CompanionMode implements StringRepresentable {
    FOLLOW("follow"),
    STAY("stay"),
    /** Free idle wander near owner — no glued follow pathing. */
    WANDER("wander"),
    PATROL("patrol"),
    GUARD("guard"),
    SIT("sit"),
    HOME("home"),
    TASK("task");

    private final String name;

    CompanionMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static CompanionMode byName(String value) {
        for (CompanionMode mode : values()) {
            if (mode.name.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return FOLLOW;
    }
}
