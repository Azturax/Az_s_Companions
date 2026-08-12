package com.azscompanions.entity;

public enum FabricCompanionMode {
    FOLLOW, STAY, WANDER, GUARD, SIT, HOME, TASK;

    public static FabricCompanionMode byName(String value) {
        for (FabricCompanionMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return FOLLOW;
    }
}
