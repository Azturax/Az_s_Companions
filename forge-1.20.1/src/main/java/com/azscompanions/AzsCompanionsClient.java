package com.azscompanions;

import com.azscompanions.client.ModKeyMappings;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.client.model.KonEarsModel;
import com.azscompanions.client.renderer.CompanionRenderer;
import com.azscompanions.client.renderer.KonAwareBedRenderer;
import com.azscompanions.client.renderer.KonEarsLayer;
import com.azscompanions.client.screen.CompanionInventoryScreen;
import com.azscompanions.client.screen.CompanionManagementScreen;
import com.azscompanions.client.screen.CompanionSelectionScreen;
import com.azscompanions.client.voice.ClientVoiceController;
import com.azscompanions.registry.ModBlockEntities;
import com.azscompanions.registry.ModEntities;
import com.azscompanions.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AzsCompanions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AzsCompanionsClient {
    private AzsCompanionsClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientVoiceController.init();
            MenuScreens.register(ModMenus.COMPANION_SELECTION.get(), CompanionSelectionScreen::new);
            MenuScreens.register(ModMenus.COMPANION_MANAGEMENT.get(), CompanionManagementScreen::new);
            MenuScreens.register(ModMenus.COMPANION_INVENTORY.get(), CompanionInventoryScreen::new);
            com.azscompanions.compat.map.MapCompatClientBridge.syncFromClientConfig();
            com.azscompanions.compat.fancyanim.FancyAnimClientBridge.syncFromClientConfig();
        });
        AzsCompanions.LOGGER.info("Az's Companions client ready");
    }

    @SubscribeEvent
    public static void onRegisterKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        ModKeyMappings.register(event);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FeminineCompanionModel.LAYER_WIDE, () -> FeminineCompanionModel.createBodyLayer(false));
        event.registerLayerDefinition(FeminineCompanionModel.LAYER_SLIM, () -> FeminineCompanionModel.createBodyLayer(true));
        event.registerLayerDefinition(KonEarsModel.LAYER, KonEarsModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new KonEarsLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMPANION.get(), CompanionRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.KON_BED.get(), KonAwareBedRenderer::new);
    }
}
