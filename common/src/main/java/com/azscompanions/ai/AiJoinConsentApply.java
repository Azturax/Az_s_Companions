package com.azscompanions.ai;

import com.azscompanions.admin.AdminAiConfigSnapshot;
import com.azscompanions.admin.LlmProviderProfile;

/**
 * Shared server-side apply for join-time LLM consent.
 * Local-probe Yes enables a personal LLM profile without forcing Use server LLM;
 * server-offer Yes opts the host into shared {@code serverLlmOnly}.
 */
public final class AiJoinConsentApply {
    public static final String TIP =
            "Companion AI ready — use /ask or /az ask to talk to your companion. "
                    + "Or configure your own local/remote LLM in /az admin → AI Config.";

    private AiJoinConsentApply() {
    }

    /**
     * Merge consent into settings.
     * <ul>
     *   <li>{@code applyProfile=true} — enable a discovered local LLM for personal use;
     *       does <em>not</em> turn on {@code serverLlmOnly}.</li>
     *   <li>{@code applyProfile=false} — opt into shared Use server LLM ({@code serverLlmOnly=true}).</li>
     * </ul>
     */
    public static CompanionAiSettings apply(CompanionAiSettings current, String suggestProfile, boolean applyProfile) {
        CompanionAiSettings base = current == null ? new CompanionAiSettings() : current.copy();
        if (!applyProfile || suggestProfile == null || suggestProfile.isBlank()) {
            base.setServerLlmOnly(true);
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
        // Keep personal mode — host can toggle Use server LLM later to share with LAN/friends
        snap.setServerLlmOnly(base.serverLlmOnly());
        return snap.mergeInto(base);
    }
}
