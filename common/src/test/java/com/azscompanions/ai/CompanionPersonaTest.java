package com.azscompanions.ai;

import com.azscompanions.cci.CciCompanionParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionPersonaTest {
    @Test
    void cciWhoWhatHowMarksInitializedAndSkipsOnboarding() {
        CciCompanionParams params = CciCompanionParams.parse(
                "whoAmI=A knight;whatAmIDoing=Guarding the base;howWillIBe=Stoic and loyal");
        assertTrue(CompanionPersona.hasPersonaKeys(params));
        CompanionPersona merged = CompanionPersona.EMPTY.mergeFromCci(params);
        assertTrue(merged.initialized());
        assertEquals("A knight", merged.whoAmI());
        assertEquals("Guarding the base", merged.whatAmIDoing());
        assertEquals("Stoic and loyal", merged.howWillIBe());
        assertFalse(CompanionPersona.shouldOfferOnboarding(false, false, merged.initialized()));
    }

    @Test
    void aliasesWhoWhatHowWork() {
        CompanionPersona p = CompanionPersona.EMPTY.mergeFromCci(
                CciCompanionParams.parse("who=Scout;what=Mining;how=Cheerful"));
        assertEquals("Scout", p.whoAmI());
        assertEquals("Mining", p.whatAmIDoing());
        assertEquals("Cheerful", p.howWillIBe());
        assertTrue(p.initialized());
    }

    @Test
    void promptAppendixInjectsPersona() {
        CompanionPersona p = new CompanionPersona("Kon", "Following", "Warm", "", "", "", true);
        String appendix = p.promptAppendix();
        assertTrue(appendix.contains("Who you are: Kon"));
        assertTrue(appendix.contains("What you are doing: Following"));
        assertTrue(appendix.contains("How you will be: Warm"));
        String prompt = new CompanionAiSettings().formatSystemPrompt(
                "Kon", "player", "", false, true, "PASSIVE", p);
        assertTrue(prompt.contains("Who you are: Kon"));
    }

    @Test
    void clearKeepsInitialized() {
        CompanionPersona cleared = new CompanionPersona("x", "y", "z", "", "", "", true).cleared();
        assertTrue(cleared.initialized());
        assertTrue(cleared.isBlank());
        assertFalse(CompanionPersona.shouldOfferOnboarding(false, false, cleared.initialized()));
    }

    @Test
    void fightSpawnAndChildrenSkipOnboarding() {
        assertFalse(CompanionPersona.shouldOfferOnboarding(true, false, false));
        assertFalse(CompanionPersona.shouldOfferOnboarding(false, true, false));
        assertTrue(CompanionPersona.shouldOfferOnboarding(false, false, false));
    }
}
