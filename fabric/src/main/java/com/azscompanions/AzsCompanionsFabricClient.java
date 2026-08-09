package com.azscompanions;

import com.azscompanions.client.FabricModKeyMappings;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.client.model.KonEarsModel;
import com.azscompanions.client.renderer.FabricCompanionRenderer;
import com.azscompanions.client.renderer.KonEarsLayer;
import com.azscompanions.client.screen.FabricCompanionInventoryScreen;
import com.azscompanions.client.screen.FabricCompanionRadialScreen;
import com.azscompanions.client.screen.FabricCompanionSelectionScreen;
import com.azscompanions.client.screen.FabricRadialCommandScreen;
import com.azscompanions.registry.FabricModEntities;
import com.azscompanions.registry.FabricModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class AzsCompanionsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(FeminineCompanionModel.LAYER_WIDE,
                () -> FeminineCompanionModel.createBodyLayer(false));
        EntityModelLayerRegistry.registerModelLayer(FeminineCompanionModel.LAYER_SLIM,
                () -> FeminineCompanionModel.createBodyLayer(true));
        EntityModelLayerRegistry.registerModelLayer(KonEarsModel.LAYER, KonEarsModel::createBodyLayer);
        EntityRendererRegistry.register(FabricModEntities.COMPANION, FabricCompanionRenderer::new);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    if (entityRenderer instanceof PlayerRenderer playerRenderer) {
                        registrationHelper.register(new KonEarsLayer(playerRenderer, context.getModelSet()));
                    }
                });
        MenuScreens.register(FabricModScreenHandlers.SELECTION, FabricCompanionSelectionScreen::new);
        MenuScreens.register(FabricModScreenHandlers.RADIAL, FabricRadialCommandScreen::new);
        MenuScreens.register(FabricModScreenHandlers.INVENTORY, FabricCompanionInventoryScreen::new);

        FabricModKeyMappings.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            while (FabricModKeyMappings.OPEN_RADIAL.consumeClick()) {
                if (client.screen == null) {
                    FabricCompanionRadialScreen.openForOwnedCompanion();
                }
            }
        });

        AzsCompanionsFabric.LOGGER.info("Az's Companions (Fabric) client ready");
    }
}
