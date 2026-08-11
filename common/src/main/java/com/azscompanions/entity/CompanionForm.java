package com.azscompanions.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Visual / collision form of a companion. Gameplay (owner, modes, charm, CCI) stays on the
 * companion entity; only model, texture, and size change.
 */
public enum CompanionForm {
    PLAYER(FormGroup.PLAYER, 0.6f, 1.8f),
    CHICKEN(FormGroup.ANIMAL, 0.4f, 0.7f),
    WOLF(FormGroup.ANIMAL, 0.6f, 0.85f),
    CAT(FormGroup.ANIMAL, 0.6f, 0.7f),
    COW(FormGroup.ANIMAL, 0.9f, 1.4f),
    PIG(FormGroup.ANIMAL, 0.9f, 0.9f),
    SHEEP(FormGroup.ANIMAL, 0.9f, 1.3f),
    FOX(FormGroup.ANIMAL, 0.6f, 0.7f),
    RABBIT(FormGroup.ANIMAL, 0.4f, 0.5f),
    BEE(FormGroup.ANIMAL, 0.7f, 0.6f),
    ZOMBIE(FormGroup.HOSTILE, 0.6f, 1.95f),
    SKELETON(FormGroup.HOSTILE, 0.6f, 1.99f),
    SPIDER(FormGroup.HOSTILE, 1.4f, 0.9f),
    ENDERMAN(FormGroup.HOSTILE, 0.6f, 2.9f),
    HUSK(FormGroup.HOSTILE, 0.6f, 1.95f),
    STRAY(FormGroup.HOSTILE, 0.6f, 1.99f),
    /** Soft glowing flying orb — custom color/brightness/float/offset; no mob proxy. */
    GLOWING_ORB(FormGroup.SPECIAL, 0.45f, 0.45f);

    public enum FormGroup {
        PLAYER,
        ANIMAL,
        HOSTILE,
        SPECIAL
    }

    private final FormGroup group;
    private final float width;
    private final float height;

    CompanionForm(FormGroup group, float width, float height) {
        this.group = group;
        this.width = width;
        this.height = height;
    }

    public FormGroup group() {
        return group;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public boolean isPlayer() {
        return this == PLAYER;
    }

    /** Glowing flying orb form (billboard render + dynamic light + always-air follow). */
    public boolean isOrb() {
        return this == GLOWING_ORB;
    }

    /**
     * Forms that use vanilla humanoid armor layers (player mesh or zombie-family / skeleton / enderman proxies).
     * True animals, spider, and orbs have no humanoid armor layers — do not accept plate armor for them.
     */
    public boolean supportsHumanoidArmor() {
        return switch (this) {
            case PLAYER, ZOMBIE, SKELETON, HUSK, STRAY, ENDERMAN -> true;
            default -> false;
        };
    }

    /** Wolf form can wear {@code AnimalArmorItem} canine (wolf) armor via the chest inventory slot → BODY. */
    public boolean supportsWolfArmor() {
        return this == WOLF;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayLabel() {
        if (this == GLOWING_ORB) {
            return "Glowing Orb";
        }
        String raw = name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    public static CompanionForm byName(String value) {
        if (value == null || value.isBlank()) {
            return PLAYER;
        }
        String key = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("ORB".equals(key) || "GLOWINGORB".equals(key.replace("_", ""))) {
            return GLOWING_ORB;
        }
        try {
            return CompanionForm.valueOf(key);
        } catch (IllegalArgumentException ex) {
            return PLAYER;
        }
    }

    public static List<CompanionForm> byGroup(FormGroup group) {
        return Arrays.stream(values()).filter(f -> f.group == group).toList();
    }
}
