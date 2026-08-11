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
    STRAY(FormGroup.HOSTILE, 0.6f, 1.99f);

    public enum FormGroup {
        PLAYER,
        ANIMAL,
        HOSTILE
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

    /**
     * Forms that use vanilla humanoid armor layers (player mesh or zombie-family / skeleton / enderman proxies).
     * True animals and spider have no humanoid armor layers — do not accept plate armor for them.
     */
    public boolean supportsHumanoidArmor() {
        return switch (this) {
            case PLAYER, ZOMBIE, SKELETON, HUSK, STRAY, ENDERMAN -> true;
            default -> false;
        };
    }

    /**
     * Sit command uses the passenger / minecart bent-leg pose (same as riding) for these forms.
     * Matches {@link #supportsHumanoidArmor()} — player mesh and humanoid hostile proxies.
     */
    public boolean usesPassengerSitPose() {
        return supportsHumanoidArmor();
    }

    /**
     * Sit command uses the vanilla animal sitting animation (wolf / cat / fox).
     * Other animals stay still without a dedicated sit mesh.
     */
    public boolean usesNativeAnimalSitPose() {
        return this == WOLF || this == CAT || this == FOX;
    }

    /** Wolf form can wear {@code AnimalArmorItem} canine (wolf) armor via the chest inventory slot → BODY. */
    public boolean supportsWolfArmor() {
        return this == WOLF;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayLabel() {
        String raw = name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    public static CompanionForm byName(String value) {
        if (value == null || value.isBlank()) {
            return PLAYER;
        }
        String key = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        // Removed Glowing Orb form — migrate old saves / CCI aliases to default player form.
        String compact = key.replace("_", "");
        if ("ORB".equals(key) || "GLOWINGORB".equals(compact) || "GLOWING_ORB".equals(key)) {
            return PLAYER;
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
