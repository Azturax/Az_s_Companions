package com.koncompanions.entity;

import java.util.Locale;

/** Presentation gender for companion body mesh (default female). */
public enum CompanionGender {
    FEMALE("female"),
    MALE("male");

    private final String id;

    CompanionGender(String id) {
        this.id = id;
    }

    public String getSerializedName() {
        return id;
    }

    public boolean isMale() {
        return this == MALE;
    }

    public boolean showsBust() {
        return this == FEMALE;
    }

    public static CompanionGender byName(String value) {
        if (value == null || value.isBlank()) {
            return FEMALE;
        }
        String key = value.trim().toLowerCase(Locale.ROOT);
        for (CompanionGender gender : values()) {
            if (gender.id.equals(key) || gender.name().equalsIgnoreCase(key)) {
                return gender;
            }
        }
        return FEMALE;
    }
}
