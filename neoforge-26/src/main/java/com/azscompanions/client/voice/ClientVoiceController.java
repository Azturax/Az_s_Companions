package com.azscompanions.client.voice;

import com.azscompanions.config.ClientConfig;
import com.azscompanions.voice.DialogueCategory;
import com.azscompanions.voice.VoiceProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

/**
 * Client-only dialogue presentation: action-bar subtitles and optional Minecraft sound events.
 * Companion AI replies are text-first (owner chat); this plays canned sound cues when enabled.
 */
public final class ClientVoiceController {
    private ClientVoiceController() {
    }

    public static void init() {
        // Soft-compat for Simple Voice Chat / VoiceMod is bootstrapped from CompatBootstrap
        // (VoiceChatCompatModule) — no TTS bridge here.
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
            mc.player.sendOverlayMessage(Component.literal(line));
        }

        if (ClientConfig.ENABLE_VOICE.get()) {
            VoiceProfile profile = VoiceProfile.resolve(voiceProfileId, category);
            float volume = ClientConfig.VOICE_VOLUME.get().floatValue();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(profile.fallbackSound(), 1.0f, volume));
            var entity = mc.level.getEntity(entityId);
            if (entity != null) {
                mc.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        profile.fallbackSound(), SoundSource.NEUTRAL, volume, 1.0f, false);
            }
        }
    }
}
