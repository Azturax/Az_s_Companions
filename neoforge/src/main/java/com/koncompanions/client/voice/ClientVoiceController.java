package com.koncompanions.client.voice;

import com.koncompanions.config.ClientConfig;
import com.koncompanions.voice.DialogueCategory;
import com.koncompanions.voice.TtsVoiceAdapter;
import com.koncompanions.voice.VoiceProfile;
import com.koncompanions.voice.VoicemodBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

/**
 * Client-only voice playback: Minecraft sounds, subtitles, optional TTS / Voicemod bridge.
 */
public final class ClientVoiceController {
    private ClientVoiceController() {
    }

    public static void init() {
        VoicemodBridge.mapEvent("GREETING", "kon_greeting");
        VoicemodBridge.mapEvent("SUCCESS", "kon_success");
        VoicemodBridge.mapEvent("DANGER", "kon_danger");
    }

    public static void handleDialogue(int entityId, String categoryName, String line, String voiceProfileId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        DialogueCategory category;
        try {
            category = DialogueCategory.valueOf(categoryName);
        } catch (IllegalArgumentException ex) {
            category = DialogueCategory.IDLE;
        }

        if (ClientConfig.ENABLE_SUBTITLES.get() && mc.player != null) {
            mc.player.displayClientMessage(Component.literal(line), true);
        }

        if (ClientConfig.ENABLE_VOICE.get()) {
            VoiceProfile profile = VoiceProfile.resolve(voiceProfileId, category);
            float volume = ClientConfig.VOICE_VOLUME.get().floatValue();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(profile.fallbackSound(), 1.0f, volume));
            // Prefer positional if entity exists.
            var entity = mc.level.getEntity(entityId);
            if (entity != null) {
                mc.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        profile.fallbackSound(), SoundSource.NEUTRAL, volume, 1.0f, false);
            }
            TtsVoiceAdapter.speak(line);
        }

        VoicemodBridge.emit(categoryName, line);
    }
}
