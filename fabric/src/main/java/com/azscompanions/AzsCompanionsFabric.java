package com.azscompanions;

import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.command.FabricCompanionCommands;
import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.data.FabricCompanionDefinitionLoader;
import com.azscompanions.entity.FabricBuiltinCompanions;
import com.azscompanions.event.FabricTeamFightEvents;
import com.azscompanions.network.FabricNetworking;
import com.azscompanions.perk.SpecialPlayerPerks;
import com.azscompanions.platform.LoaderPlatform;
import com.azscompanions.registry.FabricModBlockEntities;
import com.azscompanions.registry.FabricModBlocks;
import com.azscompanions.registry.FabricModEntities;
import com.azscompanions.registry.FabricModItems;
import com.azscompanions.registry.FabricModRecipes;
import com.azscompanions.registry.FabricModScreenHandlers;
import com.azscompanions.registry.FabricModSounds;
import com.azscompanions.task.FabricTaskRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AzsCompanionsFabric implements ModInitializer {
    public static final String MOD_ID = AzsCompanionsConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final LoaderPlatform PLATFORM = LoaderPlatform.FABRIC;

    @Override
    public void onInitialize() {
        try {
            FabricServerConfig.loadAiConfig();
        } catch (Exception e) {
            LOGGER.error("Companion AI config load failed — AI stays disabled", e);
            CompanionAiRuntime.get().applySettings(new CompanionAiSettings());
        }

        FabricModBlocks.register();
        FabricModBlockEntities.register();
        FabricModItems.register();
        FabricModRecipes.register();
        FabricModEntities.register();
        FabricModSounds.register();
        FabricModScreenHandlers.register();
        FabricNetworking.register();
        FabricTeamFightEvents.register();
        FabricTaskRegistry.bootstrap();
        FabricBuiltinCompanions.registerDefaults();

        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new FabricCompanionDefinitionLoader());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                FabricCompanionCommands.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                SpecialPlayerPerks.applyPlayerPerks(player);
            }
        });

        ResourceLocation desertPyramid = ResourceLocation.withDefaultNamespace("chests/desert_pyramid");
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (desertPyramid.equals(key.location())) {
                tableBuilder.pool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(FabricModItems.COMPANION_CHARM))
                        .build());
            }
        });

        LOGGER.info("Az's Companions (Fabric) initialized — support MC {}–{} — {}",
                AzsCompanionsConstants.MIN_MINECRAFT,
                AzsCompanionsConstants.MAX_MINECRAFT,
                CompanionAiRuntime.get().statusLine());
    }
}
