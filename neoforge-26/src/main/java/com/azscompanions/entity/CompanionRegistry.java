package com.azscompanions.entity;

import com.azscompanions.AzsCompanions;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Extensible in-memory registry of companion definitions.
 * Populated by datapack reload; mods may also register via {@link com.azscompanions.api.CompanionApi}.
 */
public final class CompanionRegistry {
    public static final Identifier KON_ID =
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "kon");

    private static final Map<Identifier, CompanionDefinition> DEFINITIONS = new LinkedHashMap<>();

    private CompanionRegistry() {
    }

    public static void clear() {
        DEFINITIONS.clear();
    }

    public static void register(CompanionDefinition definition) {
        if (!definition.adultConfirmed()) {
            AzsCompanions.LOGGER.warn("Rejected companion {} — adultConfirmed must be true", definition.id());
            return;
        }
        DEFINITIONS.put(definition.id(), definition);
    }

    public static Optional<CompanionDefinition> get(Identifier id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static CompanionDefinition getOrKon(Identifier id) {
        return get(id).orElseGet(() -> get(KON_ID).orElseThrow(() ->
                new IllegalStateException("Default companion Kon is not registered")));
    }

    public static Collection<CompanionDefinition> all() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static boolean isEmpty() {
        return DEFINITIONS.isEmpty();
    }
}
