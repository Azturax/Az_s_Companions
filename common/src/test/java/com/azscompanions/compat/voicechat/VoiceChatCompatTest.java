package com.azscompanions.compat.voicechat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceChatCompatTest {
    @AfterEach
    void reset() {
        VoiceChatCompat.setPresentMods(List.of());
    }

    @Test
    void detectPresentFiltersKnownIds() {
        Set<String> loaded = Set.of("voicechat", "sodium", "voicemod");
        List<String> found = VoiceChatMods.detectPresent(loaded::contains);
        assertEquals(List.of("voicechat", "voicemod"), found);
        assertTrue(VoiceChatMods.isSimpleVoiceChatPresent(loaded::contains));
        assertTrue(VoiceChatMods.isVoiceModPresent(loaded::contains));
        assertFalse(VoiceChatMods.anyPresent(id -> false));
    }

    @Test
    void detectAndStoreTracksFamilies() {
        VoiceChatCompat.detectAndStore(Set.of("voicechat", "voicechat_api")::contains);
        assertTrue(VoiceChatCompat.isSimpleVoiceChatPresent());
        assertTrue(VoiceChatCompat.shouldApplySimpleVoiceChatHooks());
        assertFalse(VoiceChatCompat.isVoiceModPresent());
        assertEquals(List.of("voicechat", "voicechat_api"), VoiceChatCompat.presentModIds());
    }

    @Test
    void voicemodAloneIsDetectedWithoutSvcHooks() {
        VoiceChatCompat.setPresentMods(List.of(VoiceChatMods.VOICEMOD));
        assertTrue(VoiceChatCompat.isVoiceModPresent());
        assertTrue(VoiceChatCompat.isAnyVoiceCompatPresent());
        assertFalse(VoiceChatCompat.isSimpleVoiceChatPresent());
        assertFalse(VoiceChatCompat.shouldApplySimpleVoiceChatHooks());
    }

    @Test
    void looksLikeVoiceCompatMod() {
        assertTrue(VoiceChatMods.looksLikeVoiceCompatMod("voicechat"));
        assertTrue(VoiceChatMods.looksLikeVoiceCompatMod("voicemod"));
        assertTrue(VoiceChatMods.looksLikeVoiceCompatMod("SimpleVoiceChatAddon"));
        assertFalse(VoiceChatMods.looksLikeVoiceCompatMod("journeymap"));
    }

    @Test
    void referencePinDocumented() {
        assertEquals("2.6.21", VoiceChatMods.NEOFORGE_1211_REFERENCE_VERSION);
        assertEquals("voicechat-neoforge-1.21.1-2.6.21.jar", VoiceChatMods.NEOFORGE_1211_REFERENCE_JAR);
    }
}
