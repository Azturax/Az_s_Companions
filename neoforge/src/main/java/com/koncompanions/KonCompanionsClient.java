package com.koncompanions;

import com.koncompanions.client.model.FeminineCompanionModel;
import com.koncompanions.client.renderer.CompanionRenderer;
import com.koncompanions.client.renderer.KonAwareBedRenderer;
import com.koncompanions.client.screen.CompanionInventoryScreen;
import com.koncompanions.client.screen.CompanionManagementScreen;
import com.koncompanions.client.screen.CompanionSelectionScreen;
import com.koncompanions.client.screen.RadialCommandScreen;
import com.koncompanions.client.voice.ClientVoiceController;
import com.koncompanions.registry.ModEntities;
import com.koncompanions.registry.ModMenus;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = KonCompanions.MOD_ID, dist = Dist.CLIENT)
public final class KonCompanionsClient {
    public KonCompanionsClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onRegisterRenderers);
        modBus.addListener(this::onRegisterLayers);
        modBus.addListener(this::onRegisterScreens);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientVoiceController::init);
        KonCompanions.LOGGER.info("Kon Companions client ready");
    }

    private void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FeminineCompanionModel.LAYER_WIDE, () -> FeminineCompanionModel.createBodyLayer(false));
        event.registerLayerDefinition(FeminineCompanionModel.LAYER_SLIM, () -> FeminineCompanionModel.createBodyLayer(true));
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMPANION.get(), CompanionRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityType.BED, KonAwareBedRenderer::new);
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMPANION_SELECTION.get(), CompanionSelectionScreen::new);
        event.register(ModMenus.COMPANION_MANAGEMENT.get(), CompanionManagementScreen::new);
        event.register(ModMenus.COMPANION_INVENTORY.get(), CompanionInventoryScreen::new);
        event.register(ModMenus.RADIAL_COMMAND.get(), RadialCommandScreen::new);
    }
}
