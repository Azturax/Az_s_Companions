package com.koncompanions.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.koncompanions.KonCompanionsFabric;
import com.koncompanions.entity.FabricBuiltinCompanions;
import com.koncompanions.entity.FabricCompanionDefinition;
import com.koncompanions.entity.FabricCompanionRegistry;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class FabricCompanionDefinitionLoader implements SimpleSynchronousResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(KonCompanionsFabric.MOD_ID, "companion_definitions");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        FabricCompanionRegistry.clear();
        Map<ResourceLocation, Resource> resources = manager.listResources("companions", rl -> rl.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
                JsonElement json = JsonParser.parseReader(reader);
                FabricCompanionDefinition definition = FabricCompanionDefinition.CODEC
                        .parse(JsonOps.INSTANCE, json)
                        .getOrThrow();
                FabricCompanionRegistry.register(definition);
            } catch (Exception e) {
                KonCompanionsFabric.LOGGER.error("Failed loading companion {}", entry.getKey(), e);
            }
        }
        FabricBuiltinCompanions.registerDefaults();
        KonCompanionsFabric.LOGGER.info("Loaded {} companion definition(s) on Fabric", FabricCompanionRegistry.all().size());
    }
}
