package com.azscompanions.voice;

import com.azscompanions.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;

import java.util.Locale;

public record VoiceProfile(String id, SoundEvent fallbackSound) {
    public static VoiceProfile resolve(String id, DialogueCategory category) {
        SoundEvent sound = switch (category) {
            case GREETING -> ModSounds.KON_GREETING.get();
            case IDLE -> ModSounds.KON_IDLE.get();
            case TASK_PROGRESS -> ModSounds.KON_TASK_PROGRESS.get();
            case DANGER -> ModSounds.KON_DANGER.get();
            case SUCCESS -> ModSounds.KON_SUCCESS.get();
            case LOW_HEALTH -> ModSounds.KON_LOW_HEALTH.get();
            case HUNGER -> ModSounds.KON_HUNGER.get();
            case INVENTORY_FULL -> ModSounds.KON_INVENTORY_FULL.get();
            case RETURN_HOME -> ModSounds.KON_RETURN_HOME.get();
        };
        return new VoiceProfile(id == null ? "kon_soft" : id.toLowerCase(Locale.ROOT), sound);
    }
}
