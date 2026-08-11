package com.azscompanions.compat.voicechat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Known proximity / voice soft-deps (Fabric + NeoForge 1.21.1). Detection only — never hard-depends.
 * <p>
 * Primary target: <strong>Simple Voice Chat</strong> ({@code voicechat},
 * jar pattern {@code voicechat-neoforge-1.21.1-2.6.21.jar} / fabric equivalent).
 * {@code voicemod} is detected for awareness only — the desktop VoiceMod TTS bridge is not shipped.
 */
public final class VoiceChatMods {
    /** Simple Voice Chat (henkelmax) — NeoForge / Fabric / Forge. */
    public static final String VOICECHAT = "voicechat";
    /** Voicechat API stub mod id (Fabric nested / API jar). */
    public static final String VOICECHAT_API = "voicechat_api";
    /**
     * Hypothetical / third-party Minecraft bridge for VoiceMod desktop software.
     * Detected for logging; no Control-API TTS bridge is implemented.
     */
    public static final String VOICEMOD = "voicemod";

    /** Documented reference pin for Minecraft 1.21.1 NeoForge. */
    public static final String NEOFORGE_1211_REFERENCE_VERSION = "2.6.21";
    /** Documented jar name pattern for that pin. */
    public static final String NEOFORGE_1211_REFERENCE_JAR = "voicechat-neoforge-1.21.1-2.6.21.jar";

    private static final Set<String> KNOWN_IDS = Set.of(VOICECHAT, VOICECHAT_API, VOICEMOD);

    private VoiceChatMods() {
    }

    public static Set<String> knownModIds() {
        return KNOWN_IDS;
    }

    /**
     * @param isLoaded loader predicate ({@code ModList#isLoaded} / {@code FabricLoader#isModLoaded})
     * @return loaded known mod ids, stable order for logging
     */
    public static List<String> detectPresent(Predicate<String> isLoaded) {
        List<String> found = new ArrayList<>(KNOWN_IDS.size());
        for (String id : List.of(VOICECHAT, VOICECHAT_API, VOICEMOD)) {
            if (isLoaded.test(id)) {
                found.add(id);
            }
        }
        return found;
    }

    public static boolean anyPresent(Predicate<String> isLoaded) {
        return !detectPresent(isLoaded).isEmpty();
    }

    public static boolean isSimpleVoiceChatPresent(Predicate<String> isLoaded) {
        return isLoaded.test(VOICECHAT) || isLoaded.test(VOICECHAT_API);
    }

    public static boolean isVoiceModPresent(Predicate<String> isLoaded) {
        return isLoaded.test(VOICEMOD);
    }

    /** True when the id looks like Simple Voice Chat / VoiceMod family. */
    public static boolean looksLikeVoiceCompatMod(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        String id = modId.toLowerCase(Locale.ROOT);
        if (KNOWN_IDS.contains(id)) {
            return true;
        }
        return id.contains("voicechat") || id.equals("voicemod") || id.contains("simplevoice");
    }
}
