package com.azscompanions.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionDefinition;
import com.azscompanions.entity.CompanionRegistry;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads data/&lt;namespace&gt;/companions/*.json on datapack reload.
 */
public final class CompanionDefinitionReloadListener
        extends SimplePreparableReloadListener<Map<Identifier, CompanionDefinition>> {

    private static final String DIRECTORY = "companions";

    @Override
    protected Map<Identifier, CompanionDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, CompanionDefinition> loaded = new HashMap<>();
        Map<Identifier, Resource> resources = manager.listResources(DIRECTORY, rl -> rl.getPath().endsWith(".json"));

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier fileId = entry.getKey();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
                JsonElement json = JsonParser.parseReader(reader);
                CompanionDefinition definition = CompanionDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(msg -> new IllegalStateException("Failed to parse companion " + fileId + ": " + msg));
                loaded.put(definition.id(), definition);
            } catch (Exception e) {
                AzsCompanions.LOGGER.error("Failed loading companion definition {}", fileId, e);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<Identifier, CompanionDefinition> object, ResourceManager manager, ProfilerFiller profiler) {
        CompanionRegistry.clear();
        object.values().forEach(CompanionRegistry::register);
        // Ensure Kon always exists even if a datapack omits her definition.
        com.azscompanions.entity.BuiltinCompanions.registerDefaults();
        AzsCompanions.LOGGER.info("Loaded {} companion definition(s)", CompanionRegistry.all().size());
    }
}
