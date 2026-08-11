package com.azscompanions.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmEndpointProbeTest {
    @Test
    void endpointHintParsesHostPort() {
        assertEquals("127.0.0.1:4000", LlmEndpointProbe.endpointHint("http://127.0.0.1:4000/v1"));
        assertEquals("127.0.0.1:11434", LlmEndpointProbe.endpointHint(CompanionAiSettings.DEFAULT_BASE_URL));
        assertTrue(LlmEndpointProbe.isLoopbackHttp("http://127.0.0.1:4000/v1"));
        assertFalse(LlmEndpointProbe.isLoopbackHttp("https://openrouter.ai/api/v1"));
    }

    @Test
    void defaultProbeListIncludesLiteLlm() {
        assertTrue(LlmEndpointProbe.defaultProbeBaseUrls().stream()
                .anyMatch(u -> u.contains(":4000")));
    }

    @Test
    void consentApplyEnablesLocalLlmWithoutForcingServerMode() {
        CompanionAiSettings disabled = new CompanionAiSettings().setProvider(LlmProviderMode.DISABLED);
        CompanionAiSettings next = AiJoinConsentApply.apply(disabled, "litellm", true);
        assertFalse(next.serverLlmOnly());
        assertEquals(LlmProviderMode.OPENAI_COMPATIBLE, next.provider());
        assertTrue(next.baseUrl().contains("4000"));
    }

    @Test
    void consentApplyServerOfferOptsIntoSharedMode() {
        CompanionAiSettings local = new CompanionAiSettings()
                .setProvider(LlmProviderMode.LOCAL)
                .setBaseUrl(CompanionAiSettings.DEFAULT_BASE_URL)
                .setServerLlmOnly(false);
        CompanionAiSettings next = AiJoinConsentApply.apply(local, "litellm", false);
        assertTrue(next.serverLlmOnly());
        assertEquals(LlmProviderMode.LOCAL, next.provider());
    }

    @Test
    void consentApplyKeepsExistingProviderOnLocalProbe() {
        CompanionAiSettings local = new CompanionAiSettings()
                .setProvider(LlmProviderMode.LOCAL)
                .setBaseUrl(CompanionAiSettings.DEFAULT_BASE_URL)
                .setServerLlmOnly(false);
        CompanionAiSettings next = AiJoinConsentApply.apply(local, "litellm", true);
        assertFalse(next.serverLlmOnly());
        assertEquals(LlmProviderMode.LOCAL, next.provider());
    }

    @Test
    void joinOfferFromDisabledAllowsLocalProbeOnIntegrated() {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        runtime.applySettings(new CompanionAiSettings().setProvider(LlmProviderMode.DISABLED));
        AiJoinOffer offer = AiJoinOffer.fromServerRuntime(runtime, false);
        assertFalse(offer.available());
        assertTrue(offer.allowLocalProbe());
        assertTrue(offer.allowApply());
    }

    @Test
    void joinOfferSkipsWhenPersonalLlmOnIntegrated() {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        runtime.applySettings(new CompanionAiSettings()
                .setProvider(LlmProviderMode.LOCAL)
                .setServerLlmOnly(false));
        AiJoinOffer offer = AiJoinOffer.fromServerRuntime(runtime, false);
        assertFalse(offer.available());
        assertFalse(offer.allowLocalProbe());
    }

    @Test
    void joinOfferAvailableOnDedicatedWhenAiEnabled() {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        runtime.applySettings(new CompanionAiSettings()
                .setProvider(LlmProviderMode.LOCAL)
                .setServerLlmOnly(false));
        AiJoinOffer offer = AiJoinOffer.fromServerRuntime(runtime, true);
        assertTrue(offer.available());
        assertFalse(offer.allowLocalProbe());
    }
}
