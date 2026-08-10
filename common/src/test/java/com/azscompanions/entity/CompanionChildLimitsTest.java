package com.azscompanions.entity;

import com.azscompanions.cci.CciCompanionParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class CompanionChildLimitsTest {
    @Test
    void clampSpawnCountUncapped() {
        assertEquals(1, CompanionChildLimits.clampSpawnCount(0));
        assertEquals(1, CompanionChildLimits.clampSpawnCount(-3));
        assertEquals(3, CompanionChildLimits.clampSpawnCount(3));
        assertEquals(99, CompanionChildLimits.clampSpawnCount(99));
    }

    @Test
    void clampMaxChildren() {
        assertEquals(1, CompanionChildLimits.clampMaxChildren(0));
        assertEquals(3, CompanionChildLimits.clampMaxChildren(3));
        assertEquals(64, CompanionChildLimits.clampMaxChildren(99));
    }

    @Test
    void remainingSlotsRespectsConfigCap() {
        assertEquals(3, CompanionChildLimits.remainingSlots(0));
        assertEquals(3, CompanionChildLimits.remainingSlots(0, 3));
        assertEquals(1, CompanionChildLimits.remainingSlots(2, 3));
        assertEquals(0, CompanionChildLimits.remainingSlots(3, 3));
        assertEquals(0, CompanionChildLimits.remainingSlots(10, 3));
        assertEquals(64, CompanionChildLimits.remainingSlots(0, 99));
    }

    @Test
    void spawnCountFromAmount() {
        assertEquals(1, CompanionChildLimits.spawnCountFromAmount(0));
        assertEquals(1, CompanionChildLimits.spawnCountFromAmount(50));
        assertEquals(1, CompanionChildLimits.spawnCountFromAmount(100));
        assertEquals(5, CompanionChildLimits.spawnCountFromAmount(500));
        assertEquals(10, CompanionChildLimits.spawnCountFromAmount(1000));
        assertEquals(2, CompanionChildLimits.spawnCountFromAmount(500, 250));
    }

    @Test
    void paramsParseCountAndSize() {
        CciCompanionParams params = CciCompanionParams.parse("form=chicken;name=Bit;count=3;size=0.5");
        assertEquals(3, params.spawnCountOr(1));
        assertEquals(0.5f, params.bodyScaleOr(1.0f), 0.001f);
    }

    @Test
    void paramsParseMaxChildrenAndInteractionSpawnRequest() {
        assertEquals(5, CciCompanionParams.parse("maxChildren=5").maxChildrenOrNull());
        assertEquals(64, CciCompanionParams.parse("max_children=99").maxChildrenOrNull());
        assertNull(CciCompanionParams.parse("count=2").maxChildrenOrNull());
        assertEquals(5, CciCompanionParams.parse("amount=500").childSpawnRequestOr(0));
        assertEquals(5, CciCompanionParams.parse("bits=500").childSpawnRequestOr(0));
        assertEquals(2, CciCompanionParams.parse("amount=500;count=2").childSpawnRequestOr(0));
        assertEquals(1, CciCompanionParams.parse("form=chicken").childSpawnRequestOr(0));
        assertEquals(5, CciCompanionParams.parse("amount=500").childSpawnRequestOr(0, 100));
        assertEquals(2, CciCompanionParams.parse("amount=500").childSpawnRequestOr(0, 250));
    }

    @Test
    void paramsParseFollowSpacing() {
        CciCompanionParams params = CciCompanionParams.parse("followRadius=64;personalSpace=3;wanderRadius=12");
        assertEquals(64.0f, params.followRadiusOrNull(), 0.001f);
        assertEquals(3.0f, params.personalSpaceOrNull(), 0.001f);
        assertEquals(12.0f, params.wanderRadiusOrNull(), 0.001f);
    }

    @Test
    void paramsParsePersonaChunkPlayAndClaim() {
        CciCompanionParams persona = CciCompanionParams.parse(
                "whoAmI=brave wolf;whatAmIDoing=guard;howWillIBe=loyal;speech=short;quirks=tail wag");
        org.junit.jupiter.api.Assertions.assertTrue(com.azscompanions.ai.CompanionPersona.hasPersonaKeys(persona));
        CciCompanionParams chunk = CciCompanionParams.parse("chunkLoading=false");
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, chunk.chunkLoadingOrNull());
        CciCompanionParams play = CciCompanionParams.parse("mode=rush;seconds=8;role=seeker");
        assertEquals("rush", play.playModeOr("hide"));
        assertEquals(8, play.playSecondsOr(5));
        assertEquals("seeker", play.playRoleOr("hider"));
    }
}
