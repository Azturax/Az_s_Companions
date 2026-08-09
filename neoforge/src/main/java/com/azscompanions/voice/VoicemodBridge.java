package com.azscompanions.voice;

import com.azscompanions.AzsCompanions;
import com.azscompanions.config.ClientConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Optional external bridge interface for Voicemod-compatible tools.
 * Maps dialogue events to user-configured sound keys. Never required to run the mod.
 * No proprietary Voicemod SDK is bundled.
 */
public final class VoicemodBridge {
    private static final Map<String, String> EVENT_TO_SOUND_KEY = new ConcurrentHashMap<>();
    private static BiConsumer<String, String> EXTERNAL_SINK;

    private VoicemodBridge() {
    }

    public static void mapEvent(String dialogueEvent, String voicemodSoundKey) {
        EVENT_TO_SOUND_KEY.put(dialogueEvent, voicemodSoundKey);
    }

    public static void setExternalSink(BiConsumer<String, String> sink) {
        EXTERNAL_SINK = sink;
    }

    public static void emit(String dialogueEvent, String spokenLine) {
        if (!ClientConfig.ENABLE_VOICEMOD_BRIDGE.get()) {
            return;
        }
        String key = EVENT_TO_SOUND_KEY.getOrDefault(dialogueEvent, dialogueEvent);
        if (EXTERNAL_SINK != null) {
            EXTERNAL_SINK.accept(key, spokenLine);
        } else {
            AzsCompanions.LOGGER.debug("Voicemod bridge (no sink): {} -> {}", key, spokenLine);
        }
    }
}
