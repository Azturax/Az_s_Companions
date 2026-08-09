package com.koncompanions;

import com.koncompanions.api.CompanionApi;
import com.koncompanions.command.CompanionCommands;
import com.koncompanions.command.DebugCommands;
import com.koncompanions.compat.CompatBootstrap;
import com.koncompanions.entity.BuiltinCompanions;
import com.koncompanions.config.ClientConfig;
import com.koncompanions.config.CommonConfig;
import com.koncompanions.config.ServerConfig;
import com.koncompanions.data.CompanionDefinitionReloadListener;
import com.koncompanions.network.ModNetworking;
import com.koncompanions.event.CompanionChatEvents;
import com.koncompanions.event.CompanionGameEvents;
import com.koncompanions.registry.ModBlockEntities;
import com.koncompanions.registry.ModBlocks;
import com.koncompanions.registry.ModCreativeTabs;
import com.koncompanions.registry.ModEntities;
import com.koncompanions.registry.ModItems;
import com.koncompanions.registry.ModLootModifiers;
import com.koncompanions.registry.ModMenus;
import com.koncompanions.registry.ModRecipeSerializers;
import com.koncompanions.registry.ModRecipeTypes;
import com.koncompanions.registry.ModSounds;
import com.koncompanions.task.TaskRegistry;
import com.koncompanions.util.ModVersionCompat;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * Kon Companions — adult, wholesome AI adventure companions for NeoForge.
 * Authoritative task / ownership logic is always server-side.
 */
@Mod(KonCompanions.MOD_ID)
public final class KonCompanions {
    public static final String MOD_ID = KonCompanionsConstants.MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final com.koncompanions.platform.LoaderPlatform PLATFORM =
            com.koncompanions.platform.LoaderPlatform.NEOFORGE;

    public KonCompanions(IEventBus modBus, ModContainer container) {
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

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.register(CompanionGameEvents.class);
        NeoForge.EVENT_BUS.register(CompanionChatEvents.class);

        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BuiltinCompanions.registerDefaults();
            TaskRegistry.bootstrapVanillaTasks();
            CompatBootstrap.bootstrap();
            CompanionApi.lockBootstrap();
            ModVersionCompat.logSupportBanner();
            LOGGER.info("Kon Companions common setup complete (vanilla tasks + compat API ready)");
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CompanionCommands.register(event.getDispatcher());
        DebugCommands.register(event.getDispatcher());
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CompanionDefinitionReloadListener());
    }

    private void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Kon Companions loaded on server — companion limit={}", ServerConfig.MAX_COMPANIONS_PER_PLAYER.get());
    }
}
