package com.azscompanions.compat.voicechat;

import com.azscompanions.AzsCompanionsFabric;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Optional Simple Voice Chat / VoiceMod soft-compat bootstrap (Fabric 1.21.1).
 * Soft-detect only — no hard dependency on {@code voicechat} / {@code voicemod}.
 */
public final class FabricVoiceChatCompat {
    private FabricVoiceChatCompat() {
    }

    public static void bootstrap() {
        VoiceChatCompat.detectAndStore(FabricLoader.getInstance()::isModLoaded);
        if (!VoiceChatCompat.isAnyVoiceCompatPresent()) {
            return;
        }

        if (VoiceChatCompat.isSimpleVoiceChatPresent()) {
            String apiNote = VoiceChatCompat.isVoicechatApiClassPresent()
                    ? "API class present (entity audio channel not registered yet)"
                    : "mod loaded (API class not visible yet)";
            AzsCompanionsFabric.LOGGER.info(
                    "FabricVoiceChatCompat active — Simple Voice Chat soft-compat ({}; ref fabric-1.21.1-{}; {})",
                    apiNote,
                    VoiceChatMods.NEOFORGE_1211_REFERENCE_VERSION,
                    String.join(", ", VoiceChatCompat.presentModIds()));
        }
        if (VoiceChatCompat.isVoiceModPresent()) {
            AzsCompanionsFabric.LOGGER.info(
                    "FabricVoiceChatCompat — VoiceMod mod id detected; desktop VoiceMod TTS bridge is not shipped (text dialogue only)");
        }
    }
}
