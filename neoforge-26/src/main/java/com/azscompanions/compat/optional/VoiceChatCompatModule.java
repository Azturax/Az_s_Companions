package com.azscompanions.compat.optional;

import com.azscompanions.AzsCompanions;
import com.azscompanions.compat.voicechat.VoiceChatCompat;
import com.azscompanions.compat.voicechat.VoiceChatMods;
import net.neoforged.fml.ModList;

/**
 * Optional Simple Voice Chat / VoiceMod soft-compat bootstrap (NeoForge 26.2 port).
 * Detects {@code voicechat} (and {@code voicemod} for awareness) without hard-depending on either jar.
 * Companion dialogue stays text + vanilla sounds; SVC entity-audio emission is not wired yet.
 */
public final class VoiceChatCompatModule {
    private VoiceChatCompatModule() {
    }

    public static void bootstrap() {
        VoiceChatCompat.detectAndStore(ModList.get()::isLoaded);
        if (!VoiceChatCompat.isAnyVoiceCompatPresent()) {
            return;
        }

        if (VoiceChatCompat.isSimpleVoiceChatPresent()) {
            String apiNote = VoiceChatCompat.isVoicechatApiClassPresent()
                    ? "API class present (entity audio channel not registered yet)"
                    : "mod loaded (API class not visible yet)";
            AzsCompanions.LOGGER.info(
                    "VoiceChatCompatModule active — Simple Voice Chat soft-compat ({}; ref {} / {})",
                    apiNote,
                    VoiceChatMods.NEOFORGE_1211_REFERENCE_JAR,
                    String.join(", ", VoiceChatCompat.presentModIds()));
        }
        if (VoiceChatCompat.isVoiceModPresent()) {
            AzsCompanions.LOGGER.info(
                    "VoiceChatCompatModule — VoiceMod mod id detected; desktop VoiceMod TTS bridge is not shipped (text dialogue only)");
        }
    }
}
