package com.koncompanions;

import com.koncompanions.command.FabricCompanionCommands;
import com.koncompanions.data.FabricCompanionDefinitionLoader;
import com.koncompanions.entity.FabricBuiltinCompanions;
import com.koncompanions.event.FabricCompanionChatEvents;
import com.koncompanions.network.FabricNetworking;
import com.koncompanions.platform.LoaderPlatform;
import com.koncompanions.registry.FabricModBlockEntities;
import com.koncompanions.registry.FabricModBlocks;
import com.koncompanions.registry.FabricModEntities;
import com.koncompanions.registry.FabricModItems;
import com.koncompanions.registry.FabricModRecipes;
import com.koncompanions.registry.FabricModScreenHandlers;
import com.koncompanions.registry.FabricModSounds;
import com.koncompanions.task.FabricTaskRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KonCompanionsFabric implements ModInitializer {
    public static final String MOD_ID = KonCompanionsConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final LoaderPlatform PLATFORM = LoaderPlatform.FABRIC;

    @Override
    public void onInitialize() {
        FabricModBlocks.register();
        FabricModBlockEntities.register();
        FabricModItems.register();
        FabricModRecipes.register();
        FabricModEntities.register();
        FabricModSounds.register();
        FabricModScreenHandlers.register();
        FabricNetworking.register();
        FabricTaskRegistry.bootstrap();
        FabricBuiltinCompanions.registerDefaults();

        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new FabricCompanionDefinitionLoader());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                FabricCompanionCommands.register(dispatcher));

        FabricCompanionChatEvents.register();

        ResourceLocation desertPyramid = ResourceLocation.withDefaultNamespace("chests/desert_pyramid");
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (desertPyramid.equals(key.location())) {
                tableBuilder.pool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(FabricModItems.COMPANION_CHARM))
                        .build());
            }
        });

        LOGGER.info("Kon Companions (Fabric) initialized — support MC {}–{}",
                KonCompanionsConstants.MIN_MINECRAFT, KonCompanionsConstants.MAX_MINECRAFT);
    }
}
