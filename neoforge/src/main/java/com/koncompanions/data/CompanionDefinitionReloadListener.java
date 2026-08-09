package com.koncompanions.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.koncompanions.KonCompanions;
import com.koncompanions.entity.CompanionDefinition;
import com.koncompanions.entity.CompanionRegistry;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
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
        extends SimplePreparableReloadListener<Map<ResourceLocation, CompanionDefinition>> {

    private static final String DIRECTORY = "companions";

    @Override
    protected Map<ResourceLocation, CompanionDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, CompanionDefinition> loaded = new HashMap<>();
        Map<ResourceLocation, Resource> resources = manager.listResources(DIRECTORY, rl -> rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
                JsonElement json = JsonParser.parseReader(reader);
                CompanionDefinition definition = CompanionDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(msg -> new IllegalStateException("Failed to parse companion " + fileId + ": " + msg));
                loaded.put(definition.id(), definition);
            } catch (Exception e) {
                KonCompanions.LOGGER.error("Failed loading companion definition {}", fileId, e);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<ResourceLocation, CompanionDefinition> object, ResourceManager manager, ProfilerFiller profiler) {
        CompanionRegistry.clear();
        object.values().forEach(CompanionRegistry::register);
        // Ensure Kon always exists even if a datapack omits her definition.
        com.koncompanions.entity.BuiltinCompanions.registerDefaults();
        KonCompanions.LOGGER.info("Loaded {} companion definition(s)", CompanionRegistry.all().size());
    }
}
