package com.azscompanions;

import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.command.FabricCompanionCommands;
import com.azscompanions.config.FabricCommonConfig;
import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.data.FabricCompanionDefinitionLoader;
import com.azscompanions.entity.CompanionChunkLoading;
import com.azscompanions.entity.FabricBuiltinCompanions;
import com.azscompanions.loot.CompanionLootSupport;
import com.azscompanions.event.FabricCompanionAiChatEvents;
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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AzsCompanionsFabric implements ModInitializer {
    public static final String MOD_ID = AzsCompanionsConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final LoaderPlatform PLATFORM = LoaderPlatform.FABRIC;

    @Override
    public void onInitialize() {
        // AI config is applied on SERVER_STARTING so dedicated + LAN/integrated hosts
        // own the LLM; pure clients never need local provider setup.
        try {
            FabricCommonConfig.loadOrCreate();
        } catch (Exception e) {
            LOGGER.error("Common config load failed — using defaults (enableLoot={})",
                    CompanionLootSupport.DEFAULT_ENABLE_LOOT, e);
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
        FabricCompanionAiChatEvents.register();
        com.azscompanions.event.FabricCompanionRecentActionEvents.register();
        com.azscompanions.deposit.FabricDepositEvents.register();
        com.azscompanions.event.FabricCompanionLogoutEvents.register();
        com.azscompanions.event.FabricCompanionDimensionTravelEvents.register();
        com.azscompanions.ai.FabricAiJoinOfferEvents.register();
        com.azscompanions.entity.FabricCreeperCatScareEvents.register();
        com.azscompanions.entity.FabricSkeletonWolfScareEvents.register();
        FabricTaskRegistry.bootstrap();
        FabricBuiltinCompanions.registerDefaults();
        com.azscompanions.compat.FabricFtbCompat.bootstrap();
        com.azscompanions.compat.FabricHostedWorldCompat.bootstrap();
        com.azscompanions.compat.voicechat.FabricVoiceChatCompat.bootstrap();
        com.azscompanions.compat.cci.FabricCciCompatModule.bootstrapCommon();

        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new FabricCompanionDefinitionLoader());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                FabricCompanionCommands.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try {
                com.azscompanions.config.FabricAdminConfig.loadOrCreate();
            } catch (Exception e) {
                LOGGER.error("Admin config load failed — using defaults", e);
            }
            try {
                FabricServerConfig.loadAiConfig();
            } catch (Exception e) {
                LOGGER.error("Companion AI config load failed — AI stays disabled", e);
                CompanionAiRuntime.get().applySettings(new CompanionAiSettings());
            }
            CompanionAiRuntime.get().markServerContext(server.isDedicatedServer());
            CompanionChunkLoading.clearAll();
            com.azscompanions.compat.hosted.IntegratedMultiplayerCompat.refreshServerState(
                    server.isDedicatedServer(),
                    server.isPublished(),
                    server.getPlayerList().getPlayerCount());
            LOGGER.info("Az's Companions server starting — {}", CompanionAiRuntime.get().statusLine());
            var ids = com.azscompanions.task.GatherItemCatalog.newBuffer();
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                if (key != null && item != net.minecraft.world.item.Items.AIR) {
                    ids.add(key.toString());
                }
            }
            com.azscompanions.task.GatherItemCatalog.refresh(ids);
            var recipes = com.azscompanions.task.CraftRecipeCatalog.newBuffer();
            for (var recipe : server.getRecipeManager().getRecipes()) {
                var result = recipe.getResultItem(server.registryAccess());
                if (result.isEmpty()) {
                    continue;
                }
                var itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem());
                if (itemKey == null) {
                    continue;
                }
                recipes.computeIfAbsent(itemKey.toString(), k -> new java.util.ArrayList<>())
                        .add(recipe.getId().toString());
            }
            com.azscompanions.task.CraftRecipeCatalog.refresh(recipes);
            LOGGER.info("Gather catalog {} items; craft catalog {} recipes",
                    com.azscompanions.task.GatherItemCatalog.size(),
                    com.azscompanions.task.CraftRecipeCatalog.recipeCount());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CompanionAiRuntime.get().clearServerContext();
            CompanionChunkLoading.clearAll();
            com.azscompanions.deposit.DepositChestSelection.clearAll();
            com.azscompanions.ai.CompanionRecentActionMemory.clearAll();
            com.azscompanions.ai.CompanionInventoryWatchSupport.clearAll();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                SpecialPlayerPerks.applyPlayerPerks(player);
            }
        });

        ResourceLocation desertPyramid = new ResourceLocation("minecraft", "chests/desert_pyramid");
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (!CompanionLootSupport.isLootInjectionEnabled()) {
                return;
            }
            // Unique loot: 1 stack per successful roll (within 1–3 treasure policy).
            if (desertPyramid.equals(id)) {
                tableBuilder.pool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(CompanionLootSupport.TREASURE_ROLLS_MIN))
                        .when(LootItemRandomChanceCondition.randomChance(
                                CompanionLootSupport.DESERT_PYRAMID_CHARM_CHANCE))
                        .add(LootItem.lootTableItem(FabricModItems.COMPANION_CHARM))
                        .build());
            }
        });

        LOGGER.info("Az's Companions (Fabric) initialized — support MC {}–{} — AI loads on server start",
                AzsCompanionsConstants.MIN_MINECRAFT,
                AzsCompanionsConstants.MAX_MINECRAFT);
    }
}
