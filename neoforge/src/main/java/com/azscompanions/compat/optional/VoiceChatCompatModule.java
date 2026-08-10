package com.azscompanions.compat.optional;

import com.azscompanions.AzsCompanions;

/**
 * Optional Simple Voice Chat / proximity voice bridge.
 * Voice playback remains client-local and privacy-respecting.
 */
public final class VoiceChatCompatModule {
    private VoiceChatCompatModule() {
    }

    public static void bootstrap() {
        AzsCompanions.LOGGER.info("VoiceChatCompatModule active — Simple Voice Chat optional hook (no VoiceMod)");
    }
}
