package com.azscompanions.teamfight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BitGearTiersTest {
    @Test
    void tiersMatchBitFloors() {
        assertEquals(0, BitGearTiers.forBits(0).tier());
        assertEquals(0, BitGearTiers.forBits(99).tier());
        assertEquals(1, BitGearTiers.forBits(100).tier());
        assertEquals(2, BitGearTiers.forBits(250).tier());
        assertEquals(3, BitGearTiers.forBits(500).tier());
        assertEquals(4, BitGearTiers.forBits(750).tier());
        assertEquals(5, BitGearTiers.forBits(1000).tier());
        assertEquals("minecraft:stick", BitGearTiers.forBits(100).mainhand());
        assertEquals("minecraft:netherite_sword", BitGearTiers.forBits(1000).mainhand());
    }

    @Test
    void priceTableMentionsLeatherAndNetherite() {
        String table = BitGearTiers.priceTableText();
        assertTrue(table.contains("100"));
        assertTrue(table.toLowerCase().contains("leather"));
        assertTrue(table.toLowerCase().contains("netherite"));
    }
}
