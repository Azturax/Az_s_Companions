package com.azscompanions;

import com.azscompanions.client.ModKeyMappings;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.client.model.JindujunModel;
import com.azscompanions.client.model.KonEarsModel;
import com.azscompanions.client.renderer.CompanionRenderer;
import com.azscompanions.client.renderer.FlyingNimbusRenderer;
import com.azscompanions.client.renderer.KonAwareBedRenderer;
import com.azscompanions.client.renderer.KonEarsLayer;
import com.azscompanions.client.screen.CompanionInventoryScreen;
import com.azscompanions.client.screen.CompanionManagementScreen;
import com.azscompanions.client.screen.CompanionSelectionScreen;
import com.azscompanions.client.voice.ClientVoiceController;
import com.azscompanions.registry.ModBlockEntities;
import com.azscompanions.registry.ModEntities;
import com.azscompanions.registry.ModMenus;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = AzsCompanions.MOD_ID, dist = Dist.CLIENT)
public final class AzsCompanionsClient {
    public AzsCompanionsClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onRegisterRenderers);
        modBus.addListener(this::onRegisterLayers);
        modBus.addListener(this::onAddLayers);
        modBus.addListener(this::onRegisterScreens);
        modBus.addListener(ModKeyMappings::register);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientVoiceController.init();
            com.azscompanions.compat.map.MapCompatClientBridge.syncFromClientConfig();
            com.azscompanions.compat.fancyanim.FancyAnimClientBridge.syncFromClientConfig();
        });
        AzsCompanions.LOGGER.info("Az's Companions client ready");
    }

    private void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FeminineCompanionModel.LAYER_WIDE, () -> FeminineCompanionModel.createBodyLayer(false));
        event.registerLayerDefinition(FeminineCompanionModel.LAYER_SLIM, () -> FeminineCompanionModel.createBodyLayer(true));
        event.registerLayerDefinition(KonEarsModel.LAYER, KonEarsModel::createBodyLayer);
        event.registerLayerDefinition(JindujunModel.LAYER, JindujunModel::createBodyLayer);
    }

    private void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new KonEarsLayer(renderer, event.getEntityModels()));
            }
        }
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMPANION.get(), CompanionRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_NIMBUS.get(), FlyingNimbusRenderer::new);
        // Kon bed only — never replace BlockEntityType.BED (that breaks vanilla bed items).
        event.registerBlockEntityRenderer(ModBlockEntities.KON_BED.get(), KonAwareBedRenderer::new);
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMPANION_SELECTION.get(), CompanionSelectionScreen::new);
        event.register(ModMenus.COMPANION_MANAGEMENT.get(), CompanionManagementScreen::new);
        event.register(ModMenus.COMPANION_INVENTORY.get(), CompanionInventoryScreen::new);
    }
}
