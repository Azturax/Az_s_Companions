package com.azscompanions.entity;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

/**
 * Companion melee damage is locked to vanilla netherite sword Attack Damage, independent of
 * Bit gear tiers / held tool material. Weapon attribute modifiers still apply on the entity
 * for display/equipment, but {@link #withFixedMeleeDamage} cancels them for the hit.
 */
public final class CompanionCombatDamage {
    /**
     * Minecraft 1.21.1 netherite sword tooltip Attack Damage: player base 1 + sword +7 = 8.
     */
    public static final double NETHERITE_SWORD_ATTACK_DAMAGE = 8.0d;

    private CompanionCombatDamage() {
    }

    /**
     * How much to add to an attribute base so {@code currentValue + adjustment == desired}.
     */
    public static double baseAdjustmentToReach(double currentValue, double desired) {
        return desired - currentValue;
    }

    /**
     * Runs {@code attackAction} while temporarily shifting attack-damage base so the live
     * attribute value equals {@link #NETHERITE_SWORD_ATTACK_DAMAGE}.
     *
     * @param currentValue current {@code Attributes.ATTACK_DAMAGE} value (base + modifiers)
     * @param currentBase  current base value
     * @param setBase      applies a new base (must be reversible)
     * @param attackAction typically {@code super::doHurtTarget}
     */
    public static boolean withFixedMeleeDamage(
            double currentValue,
            double currentBase,
            DoubleConsumer setBase,
            BooleanSupplier attackAction
    ) {
        double delta = baseAdjustmentToReach(currentValue, NETHERITE_SWORD_ATTACK_DAMAGE);
        if (Math.abs(delta) > 1.0E-4d) {
            setBase.accept(currentBase + delta);
        }
        try {
            return attackAction.getAsBoolean();
        } finally {
            if (Math.abs(delta) > 1.0E-4d) {
                setBase.accept(currentBase);
            }
        }
    }
}
