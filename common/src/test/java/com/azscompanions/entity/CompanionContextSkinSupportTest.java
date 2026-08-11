package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionContextSkinSupportTest {
    @Test
    void playerFormPrioritySleepingOverBathing() {
        assertEquals(
                CompanionContextSkinSupport.Context.SLEEPING,
                CompanionContextSkinSupport.activeContext(true, true, true, true));
        assertEquals(
                CompanionContextSkinSupport.Context.BATHING,
                CompanionContextSkinSupport.activeContext(true, false, true, true));
        assertEquals(
                CompanionContextSkinSupport.Context.ADVENTURING,
                CompanionContextSkinSupport.activeContext(true, false, false, true));
        assertNull(CompanionContextSkinSupport.activeContext(true, false, false, false));
    }

    @Test
    void nonPlayerFormNeverActive() {
        assertNull(CompanionContextSkinSupport.activeContext(false, true, true, true));
    }

    @Test
    void resolvePrefersContextThenCustomThenBlank() {
        assertEquals(
                "url:https://example.com/sleep.png",
                CompanionContextSkinSupport.resolveRenderSkinPath(
                        true,
                        CompanionContextSkinSupport.Context.SLEEPING,
                        "url:https://example.com/sleep.png",
                        "local:bath.png",
                        "local:adv.png",
                        "player:11111111-1111-1111-1111-111111111111"));
        assertEquals(
                "player:11111111-1111-1111-1111-111111111111",
                CompanionContextSkinSupport.resolveRenderSkinPath(
                        true,
                        CompanionContextSkinSupport.Context.SLEEPING,
                        "",
                        "local:bath.png",
                        "local:adv.png",
                        "player:11111111-1111-1111-1111-111111111111"));
        assertEquals(
                "",
                CompanionContextSkinSupport.resolveRenderSkinPath(
                        true, null, "", "", "", ""));
        assertEquals(
                "player:11111111-1111-1111-1111-111111111111",
                CompanionContextSkinSupport.resolveRenderSkinPath(
                        false,
                        CompanionContextSkinSupport.Context.SLEEPING,
                        "url:https://example.com/sleep.png",
                        "",
                        "",
                        "player:11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void sanitizeUrlAndLocal() {
        assertEquals("url:https://cdn.example/a.png",
                CompanionContextSkinSupport.sanitize("https://cdn.example/a.png"));
        assertEquals("local:outfits/sleep.png",
                CompanionContextSkinSupport.sanitize("local:outfits\\sleep.png"));
        assertEquals("", CompanionContextSkinSupport.sanitize("local:../secret.png"));
        assertEquals("", CompanionContextSkinSupport.sanitize("javascript:alert(1)"));
        assertTrue(CompanionContextSkinSupport.isUrlSkin("url:https://x"));
        assertTrue(CompanionContextSkinSupport.isLocalSkin("local:a.png"));
        assertFalse(CompanionContextSkinSupport.isBathing(true, true));
        assertTrue(CompanionContextSkinSupport.isBathing(false, true));
    }
}
