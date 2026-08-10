package com.azscompanions;

import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.api.CompanionApi;
import com.azscompanions.command.CompanionCommands;
import com.azscompanions.command.DebugCommands;
import com.azscompanions.compat.CompatBootstrap;
import com.azscompanions.entity.BuiltinCompanions;
import com.azscompanions.config.AiConfig;
import com.azscompanions.config.ClientConfig;
import com.azscompanions.config.CommonConfig;
import com.azscompanions.config.ServerConfig;
import com.azscompanions.data.CompanionDefinitionReloadListener;
import com.azscompanions.network.ModNetworking;
import com.azscompanions.event.CompanionAiChatEvents;
import com.azscompanions.event.CompanionGameEvents;
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
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        // Reload listeners: AddReloadListenerEvent pending NeoForge 26.2 event rename\r\n        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.register(CompanionGameEvents.class);
        NeoForge.EVENT_BUS.register(CompanionAiChatEvents.class);
        NeoForge.EVENT_BUS.register(TeamFightGameEvents.class);

        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, AiConfig.SPEC, AiConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private void onModConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == AiConfig.SPEC) {
            CompanionAiRuntime.get().applySettings(AiConfig.toAiSettings());
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

    private void onAddReloadListenersPlaceholder() { /* companion definitions still register via BuiltinCompanions */ }

    private void onServerStarting(ServerStartingEvent event) {
        CompanionAiRuntime.get().applySettings(AiConfig.toAiSettings());
        CompanionAiRuntime.get().markServerContext(event.getServer().isDedicatedServer());
        LOGGER.info("Az's Companions loaded on server — companions per player={} — {}",
                ServerConfig.MAX_COMPANIONS_PER_PLAYER.get(),
                CompanionAiRuntime.get().statusLine());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        CompanionAiRuntime.get().clearServerContext();
    }
}
