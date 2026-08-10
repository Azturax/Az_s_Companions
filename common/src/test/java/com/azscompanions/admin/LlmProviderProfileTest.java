package com.azscompanions.admin;

import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.LlmProviderMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmProviderProfileTest {
    @Test
    void presetsFillProviderAndBaseUrl() {
        AdminAiConfigSnapshot snap = new AdminAiConfigSnapshot();
        LlmProviderProfile.LOCAL_LM_STUDIO.applyTo(snap);
        assertEquals("local", snap.provider());
        assertEquals("http://127.0.0.1:1234/v1", snap.baseUrl());
        assertFalse(LlmProviderProfile.LOCAL_LM_STUDIO.allowsFreeProviderFields());

        LlmProviderProfile.OPENROUTER.applyTo(snap);
        assertEquals("openai_compatible", snap.provider());
        assertTrue(snap.baseUrl().contains("openrouter"));

        LlmProviderProfile.MCP_HTTP.applyTo(snap);
        assertEquals("mcp", snap.provider());
        assertEquals("http://127.0.0.1:3001/mcp", snap.mcpUrl());
    }

    @Test
    void customIsNoOpOnFields() {
        AdminAiConfigSnapshot snap = new AdminAiConfigSnapshot()
                .setProvider("openai_compatible")
                .setBaseUrl("https://example.com/v1")
                .setModel("keep-me");
        LlmProviderProfile.CUSTOM.applyTo(snap);
        assertEquals("openai_compatible", snap.provider());
        assertEquals("https://example.com/v1", snap.baseUrl());
        assertEquals("keep-me", snap.model());
        assertTrue(LlmProviderProfile.CUSTOM.allowsFreeProviderFields());
    }

    @Test
    void detectMatchesPresetsElseCustom() {
        AdminAiConfigSnapshot ollama = new AdminAiConfigSnapshot()
                .setProvider("local")
                .setBaseUrl("http://127.0.0.1:11434/v1");
        assertEquals(LlmProviderProfile.LOCAL_OLLAMA, LlmProviderProfile.detect(ollama));

        AdminAiConfigSnapshot odd = new AdminAiConfigSnapshot()
                .setProvider("local")
                .setBaseUrl("http://10.0.0.5:8080/v1");
        assertEquals(LlmProviderProfile.CUSTOM, LlmProviderProfile.detect(odd));

        assertEquals(LlmProviderProfile.DISABLED, LlmProviderProfile.detect(
                new AdminAiConfigSnapshot().setProvider("disabled")));
    }

    @Test
    void mergeAndValidate() {
        CompanionAiSettings base = new CompanionAiSettings()
                .setProvider(LlmProviderMode.DISABLED)
                .setEnableAiActions(false);
        AdminAiConfigSnapshot snap = AdminAiConfigSnapshot.fromSettings(base);
        snap.applyProfile(LlmProviderProfile.GROQ);
        snap.setEnableAiActions(true);
        CompanionAiSettings merged = snap.mergeInto(base);
        assertEquals(LlmProviderMode.OPENAI_COMPATIBLE, merged.provider());
        assertTrue(merged.baseUrl().contains("groq"));
        assertTrue(merged.enableAiActions());
        assertNull(snap.validate());

        snap.setBaseUrl("not-a-url");
        assertEquals("baseUrl must start with http:// or https://", snap.validate());
    }

    @Test
    void whitelistMatchesUuidAndName() {
        UUID id = UUID.fromString("11111111-2222-3333-4444-555555555555");
        assertTrue(AzAdminWhitelist.matches(List.of(id.toString()), id, "Steve"));
        assertTrue(AzAdminWhitelist.matches(List.of("steve"), id, "Steve"));
        assertTrue(AzAdminWhitelist.matchesAny(List.of(), List.of("Alex"), id, "Alex"));
        assertFalse(AzAdminWhitelist.matches(List.of("other"), id, "Steve"));
    }
}
