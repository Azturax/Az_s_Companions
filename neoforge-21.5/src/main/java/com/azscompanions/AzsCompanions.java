package com.azscompanions;

import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.api.CompanionApi;
import com.azscompanions.command.CompanionCommands;
import com.azscompanions.command.DebugCommands;
import com.azscompanions.compat.CompatBootstrap;
import com.azscompanions.entity.BuiltinCompanions;
import com.azscompanions.entity.CompanionChunkLoading;
import com.azscompanions.config.AiConfig;
import com.azscompanions.config.ClientConfig;
import com.azscompanions.config.CommonConfig;
import com.azscompanions.config.ServerConfig;
import com.azscompanions.data.CompanionDefinitionReloadListener;
import com.azscompanions.loot.CompanionLootSupport;
import com.azscompanions.network.ModNetworking;
import com.azscompanions.event.CompanionAiChatEvents;
import com.azscompanions.event.CompanionGameEvents;
import com.azscompanions.event.DepositSelectionEvents;
import com.azscompanions.event.TeamFightGameEvents;
import com.azscompanions.registry.ModBlockEntities;
import com.azscompanions.registry.ModBlocks;
import com.azscompanions.registry.ModCreativeTabs;
import com.azscompanions.registry.ModEntities;
import com.azscompanions.registry.ModItems;
import com.azscompanions.registry.ModLootModifiers;
import com.azscompanions.registry.ModMenus;
import com.azscompanions.registry.ModRecipeSerializers;
import com.azscompanions.registry.ModRecipeTypes;
import com.azscompanions.registry.ModSounds;
import com.azscompanions.task.TaskRegistry;
import com.azscompanions.util.ModVersionCompat;
import com.azscompanions.world.CompanionChunkTickets;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Az's Companions — adult, wholesome AI adventure companions for NeoForge.
 * Authoritative task / ownership logic is always server-side.
 */
@Mod(AzsCompanions.MOD_ID)
public final class AzsCompanions {
    public static final String MOD_ID = AzsCompanionsConstants.MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final com.azscompanions.platform.LoaderPlatform PLATFORM =
            com.azscompanions.platform.LoaderPlatform.NEOFORGE;

    public AzsCompanions(IEventBus modBus, ModContainer container) {
        ModEntities.ENTITY_TYPES.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModLootModifiers.SERIALIZERS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipeTypes.RECIPE_TYPES.register(modBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(ModNetworking::register);
        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(this::onModConfig);
        modBus.addListener(CompanionChunkTickets::register);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.register(CompanionGameEvents.class);
        NeoForge.EVENT_BUS.register(CompanionAiChatEvents.class);
        NeoForge.EVENT_BUS.register(com.azscompanions.event.CompanionRecentActionEvents.class);
        NeoForge.EVENT_BUS.register(TeamFightGameEvents.class);
        NeoForge.EVENT_BUS.register(DepositSelectionEvents.class);
        NeoForge.EVENT_BUS.register(com.azscompanions.event.CompanionLogoutEvents.class);
        NeoForge.EVENT_BUS.register(com.azscompanions.event.CompanionDimensionTravelEvents.class);
        NeoForge.EVENT_BUS.register(com.azscompanions.event.CompanionCreeperCatScareEvents.class);
        NeoForge.EVENT_BUS.register(com.azscompanions.event.CompanionSkeletonWolfScareEvents.class);
        com.azscompanions.ai.NeoAiJoinOfferEvents.bootstrap();

        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, AiConfig.SPEC, AiConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private void onModConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == CommonConfig.SPEC) {
            CompanionLootSupport.setLootInjectionEnabled(CommonConfig.ENABLE_LOOT.get());
        }
        if (event.getConfig().getSpec() == AiConfig.SPEC) {
            CompanionAiRuntime.get().applySettings(AiConfig.toAiSettings());
        }
        if (event.getConfig().getSpec() == ClientConfig.SPEC) {
            com.azscompanions.compat.map.MapCompatModule.trySyncClientSettings();
            com.azscompanions.compat.fancyanim.FancyAnimCompatModule.trySyncClientSettings();
            com.azscompanions.compat.dynamiclights.DynamicLightsCompatModule.trySyncClientSettings();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BuiltinCompanions.registerDefaults();
            TaskRegistry.bootstrapVanillaTasks();
            CompatBootstrap.bootstrap();
            CompanionApi.lockBootstrap();
            ModVersionCompat.logSupportBanner();
            LOGGER.info("Az's Companions common setup complete (vanilla tasks + compat API ready)");
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CompanionCommands.register(event.getDispatcher());
        DebugCommands.register(event.getDispatcher());
    }

    private void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "companion_definitions"),
                new CompanionDefinitionReloadListener());
    }

    private void onServerStarting(ServerStartingEvent event) {
        CompanionAiRuntime.get().applySettings(AiConfig.toAiSettings());
        CompanionAiRuntime.get().markServerContext(event.getServer().isDedicatedServer());
        CompanionChunkLoading.clearAll();
        var server = event.getServer();
        com.azscompanions.compat.hosted.IntegratedMultiplayerCompat.refreshServerState(
                server.isDedicatedServer(),
                server.isPublished(),
                server.getPlayerList().getPlayerCount());
        LOGGER.info("Az's Companions loaded on server — companions per player={} — {}",
                ServerConfig.MAX_COMPANIONS_PER_PLAYER.get(),
                CompanionAiRuntime.get().statusLine());
        var ids = com.azscompanions.task.GatherItemCatalog.newBuffer();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (key != null && item != net.minecraft.world.item.Items.AIR) {
                ids.add(key.toString());
            }
        }
        com.azscompanions.task.GatherItemCatalog.refresh(ids);
        LOGGER.info("Gather item catalog: {} items", com.azscompanions.task.GatherItemCatalog.size());
        var recipes = com.azscompanions.task.CraftRecipeCatalog.newBuffer();
        var displayContext = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(
                event.getServer().overworld());
        for (var holder : event.getServer().getRecipeManager().getRecipes()) {
            var displays = holder.value().display();
            if (displays.isEmpty()) {
                continue;
            }
            var result = displays.getFirst().result().resolveForFirstStack(displayContext);
            if (result.isEmpty()) {
                continue;
            }
            var itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem());
            if (itemKey == null) {
                continue;
            }
            recipes.computeIfAbsent(itemKey.toString(), k -> new java.util.ArrayList<>())
                    .add(holder.id().location().toString());
        }
        com.azscompanions.task.CraftRecipeCatalog.refresh(recipes);
        LOGGER.info("Craft recipe catalog: {} recipes → {} results",
                com.azscompanions.task.CraftRecipeCatalog.recipeCount(),
                com.azscompanions.task.CraftRecipeCatalog.resultCount());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        CompanionAiRuntime.get().clearServerContext();
        CompanionChunkLoading.clearAll();
        com.azscompanions.ai.CompanionRecentActionMemory.clearAll();
        com.azscompanions.ai.CompanionInventoryWatchSupport.clearAll();
    }
}
