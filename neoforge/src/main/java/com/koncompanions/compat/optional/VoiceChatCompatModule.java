package com.koncompanions.compat.optional;

import com.koncompanions.KonCompanions;

/**
 * Optional Simple Voice Chat / proximity voice bridge.
 * Voice playback remains client-local and privacy-respecting.
 */
public final class VoiceChatCompatModule {
    private VoiceChatCompatModule() {
    }

    public static void bootstrap() {
        KonCompanions.LOGGER.info("VoiceChatCompatModule active — no proprietary Voicemod code bundled");
    }
}
