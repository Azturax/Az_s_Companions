package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionCombatDamageTest {
    @Test
    void matchesNetheriteSwordTooltipTotal() {
        assertEquals(8.0d, CompanionCombatDamage.NETHERITE_SWORD_ATTACK_DAMAGE);
    }

    @Test
    void cancelsHeldSwordStackingOnBaseFour() {
        // Old companion base 4 + netherite sword +7 → 11; adjust base down so hit is 8.
        assertEquals(-3.0d, CompanionCombatDamage.baseAdjustmentToReach(11.0d, 8.0d));
        // Stick / empty hand on base 4 → raise to 8.
        assertEquals(4.0d, CompanionCombatDamage.baseAdjustmentToReach(4.0d, 8.0d));
    }

    @Test
    void withFixedMeleeDamageRestoresBase() {
        AtomicReference<Double> base = new AtomicReference<>(4.0d);
        AtomicBoolean ran = new AtomicBoolean(false);
        boolean ok = CompanionCombatDamage.withFixedMeleeDamage(
                11.0d,
                4.0d,
                base::set,
                () -> {
                    ran.set(true);
                    assertEquals(1.0d, base.get(), 1.0E-6); // 4 + (8-11)
                    return true;
                });
        assertTrue(ok);
        assertTrue(ran.get());
        assertEquals(4.0d, base.get(), 1.0E-6);
    }
}
