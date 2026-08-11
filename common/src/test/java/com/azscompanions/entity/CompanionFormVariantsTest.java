package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionFormVariantsTest {
    @Test
    void playerAndHostilesHaveNoVariants() {
        assertFalse(CompanionFormVariants.hasVariants(CompanionForm.PLAYER));
        assertFalse(CompanionFormVariants.hasVariants(CompanionForm.ZOMBIE));
        assertFalse(CompanionFormVariants.hasVariants(CompanionForm.CHICKEN));
        assertEquals("", CompanionFormVariants.cycle(CompanionForm.PLAYER, "", 1));
    }

    @Test
    void wolfCyclesCoats() {
        assertTrue(CompanionFormVariants.hasVariants(CompanionForm.WOLF));
        assertEquals("minecraft:pale", CompanionFormVariants.defaultVariant(CompanionForm.WOLF));
        assertEquals("minecraft:woods", CompanionFormVariants.cycle(CompanionForm.WOLF, "minecraft:pale", 1));
        assertEquals("minecraft:striped", CompanionFormVariants.cycle(CompanionForm.WOLF, "minecraft:pale", -1));
        assertEquals("minecraft:chestnut", CompanionFormVariants.normalize(CompanionForm.WOLF, "chestnut"));
    }

    @Test
    void rabbitAndSheepMapToNbtIds() {
        assertEquals(0, CompanionFormVariants.rabbitTypeId("brown"));
        assertEquals(99, CompanionFormVariants.rabbitTypeId("evil"));
        assertEquals(0, CompanionFormVariants.sheepColorId("white"));
        assertEquals(14, CompanionFormVariants.sheepColorId("red"));
    }
}
