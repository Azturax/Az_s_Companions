package com.azscompanions.compat.dynamiclights;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Known 1.21.x dynamic-lighting soft-deps (Fabric + NeoForge). Detection only — never hard-depends.
 */
public final class DynamicLightsMods {
    /** LambDynamicLights full mod. */
    public static final String LAMB_DYN_LIGHTS = "lambdynlights";
    /** LambDynamicLights API-only jar (sometimes present without the runtime). */
    public static final String LAMB_DYN_LIGHTS_API = "lambdynlights_api";
    /** RyoamicLights (LDL fork / 1.21 stopgap). */
    public static final String RYOAMIC_LIGHTS = "ryoamiclights";
    /** AtomicStryker Dynamic Lights and similarly named ports. */
    public static final String DYNAMIC_LIGHTS = "dynamiclights";
    /** Alternate id used by some NeoForge ports. */
    public static final String DYNAMIC_LIGHTS_ALT = "dynamic_lights";

    private static final Set<String> KNOWN_IDS = Set.of(
            LAMB_DYN_LIGHTS,
            LAMB_DYN_LIGHTS_API,
            RYOAMIC_LIGHTS,
            DYNAMIC_LIGHTS,
            DYNAMIC_LIGHTS_ALT
    );

    private DynamicLightsMods() {
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
        for (String id : List.of(
                LAMB_DYN_LIGHTS,
                LAMB_DYN_LIGHTS_API,
                RYOAMIC_LIGHTS,
                DYNAMIC_LIGHTS,
                DYNAMIC_LIGHTS_ALT)) {
            if (isLoaded.test(id)) {
                found.add(id);
            }
        }
        return found;
    }

    public static boolean anyPresent(Predicate<String> isLoaded) {
        return !detectPresent(isLoaded).isEmpty();
    }

    /** True when the id looks like a dynamic-lights family mod (including unknown forks). */
    public static boolean looksLikeDynamicLightsMod(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        String id = modId.toLowerCase(Locale.ROOT);
        if (KNOWN_IDS.contains(id)) {
            return true;
        }
        return id.contains("dynamiclight") || id.contains("dynlight") || id.contains("ryoamic");
    }
}
