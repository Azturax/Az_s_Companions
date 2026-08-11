package com.azscompanions.ai;

import com.azscompanions.admin.AdminAiConfigSnapshot;
import com.azscompanions.admin.LlmProviderProfile;

/**
 * Shared server-side apply for join-time LLM consent (enable Use server LLM + optional profile).
 */
public final class AiJoinConsentApply {
    public static final String TIP =
            "Companion AI connected — use /ask or /az ask to talk to your companion.";

    private AiJoinConsentApply() {
    }

    /**
     * Merge consent into settings: always turn on {@code serverLlmOnly}; optionally apply a
     * local profile when AI was disabled and the client discovered a running LLM.
     */
    public static CompanionAiSettings apply(CompanionAiSettings current, String suggestProfile, boolean applyProfile) {
        CompanionAiSettings base = current == null ? new CompanionAiSettings() : current.copy();
        base.setServerLlmOnly(true);
        if (!applyProfile || suggestProfile == null || suggestProfile.isBlank()) {
            return base;
        }
        if (base.provider().isEnabled()) {
            return base;
        }
        LlmProviderProfile profile = LlmProviderProfile.fromId(suggestProfile);
        if (profile == LlmProviderProfile.CUSTOM || profile == LlmProviderProfile.DISABLED) {
            // Fall back to LiteLLM when probe said litellm / unknown local
            String id = suggestProfile.trim().toLowerCase();
            if (id.contains("ollama")) {
                profile = LlmProviderProfile.LOCAL_OLLAMA;
            } else if (id.contains("lm_studio") || id.contains("lmstudio")) {
                profile = LlmProviderProfile.LOCAL_LM_STUDIO;
            } else {
                profile = LlmProviderProfile.LITELLM;
            }
        }
        AdminAiConfigSnapshot snap = AdminAiConfigSnapshot.fromSettings(base);
        profile.applyTo(snap);
        snap.setServerLlmOnly(true);
        return snap.mergeInto(base);
    }
}
