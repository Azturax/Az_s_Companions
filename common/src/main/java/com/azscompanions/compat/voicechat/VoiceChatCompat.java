package com.azscompanions.compat.voicechat;

import java.util.List;
import java.util.function.Predicate;

/**
 * Soft facade for proximity voice mods (Simple Voice Chat + optional VoiceMod detection).
 * Companion dialogue remains text + vanilla Minecraft sounds; SVC entity audio emission is future work.
 */
public final class VoiceChatCompat {
    private static volatile boolean voiceModPresent;
    private static volatile boolean simpleVoiceChatPresent;
    private static volatile boolean voicechatApiClassPresent;
    private static volatile List<String> presentModIds = List.of();

    private VoiceChatCompat() {
    }

    public static void setPresentMods(List<String> modIds) {
        presentModIds = modIds == null ? List.of() : List.copyOf(modIds);
        simpleVoiceChatPresent = presentModIds.contains(VoiceChatMods.VOICECHAT)
                || presentModIds.contains(VoiceChatMods.VOICECHAT_API);
        voiceModPresent = presentModIds.contains(VoiceChatMods.VOICEMOD);
        if (presentModIds.isEmpty()) {
            voicechatApiClassPresent = false;
        }
    }

    public static void detectAndStore(Predicate<String> isLoaded) {
        setPresentMods(VoiceChatMods.detectPresent(isLoaded));
        voicechatApiClassPresent = probeVoicechatApiClass();
    }

    /**
     * Soft-probe for henkelmax Voice Chat API without a compile dependency.
     * Presence means the runtime jar exposed the plugin API; we do not register channels yet.
     */
    public static boolean probeVoicechatApiClass() {
        try {
            Class.forName("de.maxhenkel.voicechat.api.VoicechatApi", false, VoiceChatCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean isAnyVoiceCompatPresent() {
        return !presentModIds.isEmpty();
    }

    public static boolean isSimpleVoiceChatPresent() {
        return simpleVoiceChatPresent;
    }

    public static boolean isVoiceModPresent() {
        return voiceModPresent;
    }

    public static boolean isVoicechatApiClassPresent() {
        return voicechatApiClassPresent;
    }

    public static List<String> presentModIds() {
        return presentModIds;
    }

    /** Whether Simple Voice Chat soft hooks should be considered active (mod loaded). */
    public static boolean shouldApplySimpleVoiceChatHooks() {
        return simpleVoiceChatPresent;
    }
}
