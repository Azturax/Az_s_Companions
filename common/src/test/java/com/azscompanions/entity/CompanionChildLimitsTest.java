package com.azscompanions.entity;

import com.azscompanions.cci.CciCompanionParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CompanionChildLimitsTest {
    @Test
    void clampSpawnCount() {
        assertEquals(1, CompanionChildLimits.clampSpawnCount(0));
        assertEquals(1, CompanionChildLimits.clampSpawnCount(-3));
        assertEquals(3, CompanionChildLimits.clampSpawnCount(3));
        assertEquals(8, CompanionChildLimits.clampSpawnCount(99));
    }

    @Test
    void remainingSlotsRespectsConfigCap() {
        assertEquals(6, CompanionChildLimits.remainingSlots(0, 6));
        assertEquals(2, CompanionChildLimits.remainingSlots(4, 6));
        assertEquals(0, CompanionChildLimits.remainingSlots(6, 6));
        assertEquals(0, CompanionChildLimits.remainingSlots(10, 6));
        assertEquals(8, CompanionChildLimits.remainingSlots(0, 99)); // hard max 8
    }

    @Test
    void paramsParseCountAndSize() {
        CciCompanionParams params = CciCompanionParams.parse("form=chicken;name=Bit;count=3;size=0.5");
        assertEquals(3, params.spawnCountOr(1));
        assertEquals(0.5f, params.bodyScaleOr(1.0f), 0.001f);
    }
}
