package com.azscompanions.entity;

/**
 * Flying Nimbus / Jindujun (Überschallwolke) shared constants.
 * Rideable player-controlled cloud — not an AI pet while mounted.
 */
public final class JindujunSupport {
    public static final String ENTITY_ID = "flying_nimbus";
    public static final String ITEM_ID = "jindujun_whistle";
    public static final String NBT_OWNER = "NimbusOwner";

    /** Horizontal cruise speed while steered. */
    public static final double FLY_SPEED = 0.55d;
    /** Vertical climb/descend speed (jump / sneak). */
    public static final double VERTICAL_SPEED = 0.42d;
    /** Soft damp when no input. */
    public static final double IDLE_DAMP = 0.86d;

    /** Cloud collision box. */
    public static final float WIDTH = 1.35f;
    public static final float HEIGHT = 0.55f;

    /** Rider sits atop the fluff. */
    public static final double RIDER_Y_OFFSET = 0.48d;

    /** Archaeology brush chance in Trail Ruins (taiga only). */
    public static final float TRAIL_RUINS_LOOT_CHANCE = 0.02f;

    private JindujunSupport() {
    }
}
