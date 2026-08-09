package com.azscompanions.entity;

import com.azscompanions.AzsCompanionsFabric;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class FabricCompanionRegistry {
    public static final ResourceLocation KON_ID =
            ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "kon");

    private static final Map<ResourceLocation, FabricCompanionDefinition> DEFINITIONS = new LinkedHashMap<>();

    private FabricCompanionRegistry() {
    }

    public static void clear() {
        DEFINITIONS.clear();
    }

    public static void register(FabricCompanionDefinition definition) {
        if (!definition.adultConfirmed()) {
            AzsCompanionsFabric.LOGGER.warn("Rejected companion {} — adultConfirmed must be true", definition.id());
            return;
        }
        DEFINITIONS.put(definition.id(), definition);
    }

    public static Optional<FabricCompanionDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static FabricCompanionDefinition getOrKon(ResourceLocation id) {
        return get(id).orElseGet(() -> get(KON_ID).orElseThrow());
    }

    public static Collection<FabricCompanionDefinition> all() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }
}
